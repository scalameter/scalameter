package org.scalameter



import collection._
import util.parsing.combinator._



object Main {

  def main(args: Array[String]) {
    // initialize
    val configuration = Configuration.fromCommandLineArgs(args)
    run(configuration)
  }

  def run(configuration: Configuration) {
    // prepare initial context
    // identify test objects
    dyn.currentContext.withValue(Context.topLevel ++ configuration.context) {
      import configuration._
      // schedule benchmarks
      val testResults = for (benchname <- benches) yield {
        val bench = Class.forName(benchname).newInstance.asInstanceOf[DSL]
        bench.executeTests()
      }

      if (testResults.exists(_ == false)) sys.exit(1)
    }
  }

  case class Configuration(benches: Seq[String], context: Context)

  object Configuration extends JavaTokenParsers {

    import Key._

    def fromCommandLineArgs(args: Array[String]) = {
      def arguments: Parser[Configuration] = rep(arg) ^^ {
        case configs => configs.foldLeft(Configuration(Nil, Context.empty)) {
          case (acc, x) => Configuration(acc.benches ++ x.benches, acc.context ++ x.context)
        }
      }
      def arg: Parser[Configuration] = benches | intsetting | resdir | scopefilt | flag
      def listOf(flagname: String, shorthand: String): Parser[Seq[String]] = "-" ~ (flagname | shorthand) ~ classnames ^^ {
        case _ ~ _ ~ classnames => classnames
      }
      def classnames: Parser[Seq[String]] = repsep(classname, ":")
      def classname: Parser[String] = repsep(ident, ".") ^^ { _.mkString(".") }
      def benches: Parser[Configuration] = listOf("benches", "b") ^^ {
        case names => Configuration(names, Context.empty)
      }
      def intsetting: Parser[Configuration] = "-" ~ ident ~ (decimalNumber | "true" | "false") ^^ {
        case _ ~ "Cminwarmups" ~ num => Configuration(Nil, Context(exec.minWarmupRuns -> num.toInt))
        case _ ~ "Cmaxwarmups" ~ num => Configuration(Nil, Context(exec.maxWarmupRuns -> num.toInt))
        case _ ~ "Cruns" ~ num => Configuration(Nil, Context(exec.benchRuns -> num.toInt))
        case _ ~ "Ccolors" ~ flag => Configuration(Nil, Context(reports.colors -> flag.toBoolean))
      }
      def path: Parser[String] = opt("/") ~ repsep("""[\w\d-\.]+""".r, "/") ~ opt("/") ^^ {
        case lead ~ ps ~ trail => lead.getOrElse("") + ps.mkString("/") + trail.getOrElse("")
      }
      def resdir: Parser[Configuration] = "-" ~ "CresultDir" ~ path ^^ {
        case _ ~ _ ~ s => Configuration(Nil, Context(reports.resultDir -> s))
      }
      def stringLit = "['\"]".r ~ rep("[^'']".r) ~ "['\"]".r ^^ {
        case _ ~ cs ~ _ => cs.mkString
      }
      def scopefilt: Parser[Configuration] = "-" ~ "CscopeFilter" ~ (stringLit | failure("scopeFilter must be followed by a single or double quoted string.")) ^^ {
        case _ ~ _ ~ s => Configuration(Nil, Context(scopeFilter -> s))
      }
      def flag: Parser[Configuration] = "-" ~ ("silent" | "verbose" | "preJDK7") ^^ {
        case _ ~ "verbose" => Configuration(Nil, Context(Key.verbose -> true))
        case _ ~ "silent" => Configuration(Nil, Context(Key.verbose -> false))
        case _ ~ "preJDK7" => Configuration(Nil, Context(Key.preJDK7 -> true))
      }

      parseAll(arguments, args.mkString(" ")) match {
        case Success(result, _) => result
        case Failure(msg, _) => sys.error("failed to parse args: " + msg)
        case Error(msg, _) => sys.error("error while parsing args: " + msg)
      }
    }
  }

}















