import sbt._
import sbt.Keys._
import sbt.Package._

import sbtrelease._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.Utilities._

import scala.util.Random

object ReleaseExtras {
  object ReleaseExtrasKeys {
    val releaseBranchName = taskKey[String]("The name of the branch")

    object examples {
      val repo = settingKey[String]("Remote location of scalameter-examples")
      val tag = settingKey[String]("Tag name on ScalaMeter release")
      val tagComment = settingKey[String]("Tag comment on ScalaMeter release")
      val commitMessage = settingKey[String]("Commit message on ScalaMeter update")
      val scalaMeterVersionFile = settingKey[String]("Name of version file for ScalaMeter artifact")
      val scalaMeterVersionFileContent = settingKey[String]("Content of version file for ScalaMeter artifact")
    }
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
    st.extract.get(releaseVcs).collect {
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

  /**  This release step involves following actions:
    *  - clone scalameter-examples from `examples.repo`
    *  - bump up version in all examples to ScalaMeter release version and commit changes
    *  - tag scalameter-examples with ScalaMeter release version
    *  - bump up versions in all examples to new ScalaMeter snapshot version and commit changes
    *  - push changes to `examples.repo`
    */
  lazy val bumpUpVersionInExamples: ReleaseStep = { st: State =>
    val repo = st.extract.get(examples.repo)
    val (releaseV, nextV) = st.get(ReleaseKeys.versions).getOrElse(
      sys.error("No versions are set! Was this release part executed before inquireVersions?")
    )
    val tag = st.extract.get(examples.tag).format(releaseV)
    val comment = st.extract.get(examples.tagComment).format(releaseV)
    val commitMsg = st.extract.get(examples.commitMessage)
    val artifactVersionFile = st.extract.get(examples.scalaMeterVersionFile)
    val artifactVersionFileContent = st.extract.get(examples.scalaMeterVersionFileContent)

    val exampleDirFilter = new FileFilter {
      def accept(file: File): Boolean = IO.listFiles(file).exists(_.getName == artifactVersionFile)
    }

    def setVersions(in: File, version: String): Seq[String] = {
      IO.listFiles(in, exampleDirFilter).map { exampleDir =>
        val versionFile = new File(exampleDir, artifactVersionFile)
        st.log.info(s"Writing version '$version' to ${in.getName}/${versionFile.getName}")
        IO.write(versionFile, artifactVersionFileContent.format(version))
        versionFile.getAbsolutePath
      }
    }

    def pushChanges(vc: Git) = {
      val defaultChoice = extractDefault(st, "y")

      if (vc.hasUpstream) {
        defaultChoice orElse SimpleReader.readLine("Push changes to the remote repository (y/n)? [y] ") match {
          case Yes() | Some("") =>
            st.log.info("git push sends its console output to standard error, which will cause the next few lines to be marked as [error].")
            vc.pushChanges !! st.log
          case _ => st.log.warn("Remember to push the changes yourself!")
        }
      } else {
        st.log.info(s"Changes were NOT pushed, because no upstream branch is configured for the local branch [${vc.currentBranch}]")
      }
    }

    IO.withTemporaryDirectory { tmpDir =>
      st.log.info(s"Starting cloning $repo to $tmpDir")
      Process("git" :: "clone" :: repo :: "." :: Nil, tmpDir) !! st.log
      val examplesGit = new Git(tmpDir)

      st.log.info(s"Setting release version '$releaseV'")
      val filesWithReleaseV = setVersions(tmpDir, releaseV)
      examplesGit.add(filesWithReleaseV: _*) !! st.log
      examplesGit.commit(commitMsg.format(releaseV), sign = false) !! st.log
      examplesGit.tag(name = tag, comment = comment, sign = false) !! st.log

      st.log.info(s"Setting snapshot version '$nextV'")
      val filesWithNextV = setVersions(tmpDir, nextV)
      examplesGit.add(filesWithNextV: _*) !! st.log
      examplesGit.commit(commitMsg.format(nextV), sign = false) !! st.log

      st.log.info(s"Starting pushing changes to $repo")
      pushChanges(examplesGit)
    }

    st
  }
}
