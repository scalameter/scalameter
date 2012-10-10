package org.collperf
package utils



import org.scalatest.FunSuite



class ZipperTest extends FunSuite {

  test("Zipper.descend,ascend") {
    val zipper = Tree.Zipper.root[Int]
    assert(zipper.descend.ascend.result == Tree[Int](initialContext.value, Seq(), Seq(
      Tree(initialContext.value, Seq(), Seq())
    )))
    assert(zipper.descend.ascend.descend.ascend.result == Tree[Int](initialContext.value, Seq(), Seq(
      Tree(initialContext.value, Seq(), Seq()),
      Tree(initialContext.value, Seq(), Seq())
    )))
    assert(zipper.descend.ascend.descend.addItem(1).ascend.result == Tree[Int](initialContext.value, Seq(), Seq(
      Tree(initialContext.value, Seq(), Seq()),
      Tree(initialContext.value, Seq(1), Seq())
    )))
    assert(zipper.descend.addContext("one" -> 1).ascend.descend.addItem(1).ascend.result == Tree[Int](initialContext.value, Seq(), Seq(
      Tree(initialContext.value + ("one" -> 1), Seq(), Seq()),
      Tree(initialContext.value, Seq(1), Seq())
    )))
  }

}



