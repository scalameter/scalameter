import sbt._
import sbt.Keys._
import sbt.Package._

import sbtrelease._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._

object ReleaseExtras {
  object ReleaseExtrasKeys {
    val releaseBranchName = taskKey[String]("The name of the branch")
  }

  import ReleaseExtrasKeys._

  implicit class RichGit(git: Git) {
    def checkout(name: String): ProcessBuilder = git.cmd("checkout", name)

    def checkoutNew(name: String, from: String, force: Boolean = false): ProcessBuilder =
      git.cmd("checkout", if (force) "-B" else "-b", name, from)

    def pushBranch(branch: String, remote: String): ProcessBuilder =
      git.cmd("push", "-u", remote, branch)
  }

  private def git(st: State): Git = {
    st.extract.get(versionControlSystem).collect {
      case git: Git => git
    }.getOrElse(sys.error("Aborting release. Working directory is not a repository of a Git."))
  }

  /**  This release step involves following actions:
    *  - create version branch from current branch setting same remote
    *  - checkout version branch and push it to remote
    *  - publish artifacts using predefined sbt-release step
    *  - push changes of versioned branch to upstream using predefined sbt-release step
    *  - checkout back to former current branch
    */
  lazy val branchRelease: ReleaseStep = ReleaseStep(
    action = branchReleaseAction,
    check = st => pushChanges.check(publishArtifacts.check(st)),
    enableCrossBuild = publishArtifacts.enableCrossBuild || pushChanges.enableCrossBuild
  )

  private lazy val branchReleaseAction = { st: State =>
    val currentBranch = git(st).currentBranch
    val currentBranchRemote = git(st).trackingRemote

    val (branchState, branch) = st.extract.runTask(releaseBranchName, st)
    git(branchState).checkoutNew(branch, from = currentBranch, force = true) !! branchState.log
    if (!git(branchState).hasUpstream)
      git(branchState).pushBranch(branch, remote = currentBranchRemote) !! branchState.log

    // add manifest attribute 'Vcs-Release-Branch' to current settings
    val withManifestAttributeState = reapply(Seq[Setting[_]](
      packageOptions += ManifestAttributes("Vcs-Release-Branch" -> branch)
    ), branchState)

    val publishArtifactsState = publishArtifacts.action(withManifestAttributeState)

    val pushChangesState = pushChanges.action(publishArtifactsState)

    git(pushChangesState).checkout(currentBranch) !! pushChangesState.log

    pushChangesState
  }
}
