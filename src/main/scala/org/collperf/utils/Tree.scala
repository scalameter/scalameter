package org.collperf
package utils



import collection._



case class Tree[T](context: Context, items: Seq[T], children: Seq[Tree[T]]) {
  def foreach[U](f: T => U) {
    for (x <- items) f(x)
    for (child <- children; x <- child) f(x)
  }

  def map[S](f: T => S): Tree[S] = {
    val mappeditems = for (x <- items) yield f(x)
    val mappedchildren = for (child <- children) yield child.map(f)
    Tree(context, mappeditems, mappedchildren)
  }

  def scopes = new Traversable[(Context, Seq[T])] {
    private def recurse[U](f: ((Context, Seq[T])) => U, tree: Tree[T]) {
      f(tree.context -> tree.items)
      for (n <- tree.children) recurse(f, n)
    }
    def foreach[U](f: ((Context, Seq[T])) => U) {
      recurse(f, Tree.this)
    }
  }

  override def toString = s"Tree(${context.get(Key.executor)}, $items, $children)"
}


object Tree {

  case class Zipper[T](current: Tree[T], path: Zipper.Path[T]) {
    import Zipper._
    def addContext(kv: (String, Any)) = Zipper(current.copy(context = current.context + kv), path)
    def transformContext[T](k: String, f: T => Any) = {
      val v2 = f(current.context.properties(k).asInstanceOf[T])
      Zipper(current.copy(context = current.context + (k -> v2)), path)
    }
    def setContext(ctx: Context) = Zipper(current.copy(context = ctx), path)
    def addItem(x: T) = Zipper(current.copy(items = current.items :+ x), path)
    def descend = Zipper(Tree(current.context, Seq(), Seq()), Node(current.context, current.items, current.children, path))
    def ascend = path match {
      case Node(ctx, its: Seq[T], left: Seq[Tree[T]], up: Path[T]) => Zipper(Tree(ctx, its, left :+ current), up)
    }
    def result = current
  }

  object Zipper {
    trait Path[+T]
    case object Top extends Path[Nothing]
    case class Node[T](context: Context, items: Seq[T], left: Seq[Tree[T]], up: Path[T]) extends Path[T]

    def root[T]: Zipper[T] = Zipper(Tree(initialContext, Seq(), Seq()), Top)
  }

}

