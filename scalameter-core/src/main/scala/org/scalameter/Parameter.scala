package org.scalameter

import scala.reflect.{AnyValManifest}
import scala.util.parsing.combinator._
import scala.util.parsing.input.CharSequenceReader


case class Parameter[+T: Manifest](name: String) extends TypeHintedKey[Any] {
  require(manifest[T] != Manifest.Nothing, "Inferred type param to Nothing. Please provide correct type manually.")

  override def toString: String = name

  def typeHint: Manifest[Any] = manifest[T].asInstanceOf[Manifest[Any]]
}

object Parameter {
  private class ManifestParser extends JavaTokenParsers with PackratParsers {
    override final def skipWhitespace = false

    private lazy val value: PackratParser[AnyValManifest[_]] = {
      ("Byte" ^^^ Manifest.Byte) |
        ("Short" ^^^ Manifest.Short) |
        ("Char" ^^^ Manifest.Char) |
        ("Int" ^^^ Manifest.Int) |
        ("Long" ^^^ Manifest.Long) |
        ("Float" ^^^ Manifest.Float) |
        ("Double" ^^^ Manifest.Double) |
        ("Boolean" ^^^ Manifest.Boolean) |
        ("Unit" ^^^ Manifest.Unit)
    }

    private lazy val phantom: PackratParser[Manifest[_]] = {
      ("Any" ^^^ Manifest.Any) |
        ("Object" ^^^ Manifest.Object) |
        ("AnyRef" ^^^ Manifest.AnyRef) |
        ("AnyVal" ^^^ Manifest.AnyVal) |
        ("Null" ^^^ Manifest.Null) |
        ("Nothing" ^^^ Manifest.Nothing)
    }

    private def cls: PackratParser[String] = rep1sep(ident, ".") ^^ { case s =>
      s.mkString(".")
    }

    private lazy val singleton: PackratParser[Manifest[_]] = cls ^? { case s if s.endsWith(".type") =>
      val clazz = Class.forName(s.stripSuffix(".type")+"$")
      Manifest.singleType(clazz.getField("MODULE$").get(null))
    }

    private lazy val classType: PackratParser[Manifest[_]] = opt(manifest <~ "#") ~ cls ~ opt("[" ~> rep1sep(manifest, ", ") <~ "]") ^^ {
      case None ~ s ~ None => Manifest.classType(Class.forName(s))
      case None ~ s ~ Some(targ :: targs) => Manifest.classType(Class.forName(s), targ, targs: _*)
      case Some(p) ~ s ~ targs => Manifest.classType(p, Class.forName(s), targs.getOrElse(Nil): _*)
    }

    private lazy val array: PackratParser[Manifest[_]] = "Array" ~ "[" ~> manifest <~ "]" ^^ { case m =>
      Manifest.arrayType(m)
    }

    private lazy val wildcard: PackratParser[Manifest[_]] = "_" ~> (opt(" >: " ~> manifest) ~ opt(" <: " ~> manifest)) ^^ {
      case None ~ None => Manifest.wildcardType(Manifest.Nothing, Manifest.Any)
      case None ~ Some(m) => Manifest.wildcardType(Manifest.Nothing, m)
      case Some(m) ~ None => Manifest.wildcardType(m, Manifest.Any)
      case Some(m1) ~ Some(m2) => Manifest.wildcardType(m1, m2)
    }

    private lazy val intersectionType: PackratParser[Manifest[_]] = rep1sep(manifest, " with ") ^^ { case ms =>
      Manifest.intersectionType(ms: _*)
    }

    private[Parameter] lazy val manifest: PackratParser[Manifest[_]] =
      value | phantom | singleton | array | wildcard | classType | intersectionType
  }

  def fromString(str: String): Parameter[_] = {
    val i = str.lastIndexOf('|')
    if (i == -1) sys.error("""Invalid parameter string. It should have following form "name|manifest".""")
    val key = str.substring(0, i)
    val manifestString = str.substring(i + 1)
    val parser = new ManifestParser
    val m = parser.parse(parser.manifest, new parser.PackratReader(new CharSequenceReader(manifestString))).getOrElse(
      sys.error(s"""Invalid manifest string: "$manifestString".""")
    )
    Parameter(key)(m)
  }
}
