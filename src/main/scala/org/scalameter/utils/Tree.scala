package org.scalameter
package utils



import collection._



case class Tree[T](context: Context, items: Seq[T], children: Seq[Tree[T]]) {
  def foreach[U](f: T => U): Unit = {
    for (x <- items) f(x)
    for (child <- children; x <- child) f(x)
  }

  def map[S](f: T => S): Tree[S] = {
    val mappeditems = for (x <- items) yield f(x)
    val mappedchildren = for (child <- children) yield child.map(f)
    Tree(context, mappeditems, mappedchildren)
  }

  def filter(p: T => Boolean): Tree[T] = {
    val filtereditems = for (x <- items if p(x)) yield x
    val filteredchildren = for (child <- children) yield child.filter(p)
    Tree(context, filtereditems, filteredchildren)
  }

  def scopes: Iterable[(Context, Seq[T])] = {
    def recurse(tree: Tree[T]): Iterator[(Context, Seq[T])] =
      Iterator.single(tree.context -> tree.items) ++ tree.children.iterator.flatMap(recurse)
    recurse(this).toIterable
  }

  override def toString =
    s"Tree(${context.get(FrameworkKey.dsl.executor)}, $items, $children)"
}


object Tree {

  case class Zipper[T](current: Tree[T], path: Zipper.Path[T]) {
    import Zipper._
    def addContext[S](kv: (Key[S], S)) =
      Zipper(current.copy(context = current.context + kv), path)
    def modifyContext(f: Context => Context) =
      setContext(f(current.context))
    def setContext(ctx: Context) =
      Zipper(current.copy(context = ctx), path)
    def addItem(x: T) =
      Zipper(current.copy(items = current.items :+ x), path)
    def descend =
      Zipper(
        Tree(current.context, Seq(), Seq()),
        Node(current.context, current.items, current.children, path))
    def ascend = path match {
      case Node(ctx, its: Seq[T], left: Seq[Tree[T]], up: Path[T]) =>
        Zipper[T](Tree(ctx, its, left :+ current), up)
    }
    def result = current
  }

  object Zipper {
    trait Path[+T]
    case object Top extends Path[Nothing]
    case class Node[T](
      context: Context,
      items: Seq[T],
      left: Seq[Tree[T]],
      up: Path[T]
    ) extends Path[T]

    def root[T](initialContext: Context): Zipper[T] =
      Zipper(Tree(initialContext, Seq(), Seq()), Top)
  }

}

