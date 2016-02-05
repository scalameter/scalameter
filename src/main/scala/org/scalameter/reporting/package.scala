package org.scalameter






package object reporting {

  private[reporting] object ansi {
    val colors = currentContext(Key.reports.colors)
    def ifcolor(s: String) = if (colors) s else ""

    val red = ifcolor("\u001B[31m")
    val green = ifcolor("\u001B[32m")
    val yellow = ifcolor("\u001B[33m")
    val reset = ifcolor("\u001B[0m")
  }

}
