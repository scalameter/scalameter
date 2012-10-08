package org.collperf



import collection._
import util.parsing.combinator._



object Main {

  def main(args: Array[String]) {
    // initialize
    val configuration = Configuration.fromCommandLineArgs(args)
    run(configuration)
  }

  def run(configuration: Configuration) {
    // prepare top-level context
    // identify test objects
    // create reporters and persistors
    configurationContext = Context.machine ++ configuration.context + (Key.persistor -> configuration.persistor)
    currentContext.value = configurationContext
    import configuration._

    // schedule benchmarks
    for (benchname <- benches) {
      val bench = Class.forName(benchname).newInstance.asInstanceOf[PerformanceTest]
      bench.reporter.flush()
    }
  }

  case class Configuration(benches: Seq[String], persistor: Persistor, context: Context)

  object Configuration extends JavaTokenParsers {

    private def persistorFor(name: String) = name match {
      case "None" => Persistor.None
    }

    def fromCommandLineArgs(args: Array[String]) = {
      def arguments: Parser[Configuration] = rep(arg) ^^ {
        case configs => configs.foldLeft(Configuration(Nil, Persistor.None, Context.empty)) {
          case (acc, x) => Configuration(acc.benches ++ x.benches, x.persistor, acc.context ++ x.context)
        }
      }
      def arg: Parser[Configuration] = benches | persistor | intsetting | flag
      def listOf(flagname: String, shorthand: String): Parser[Seq[String]] = "-" ~ (flagname | shorthand) ~ classnames ^^ {
        case _ ~ _ ~ classnames => classnames
      }
      def classnames: Parser[Seq[String]] = repsep(classname, ":")
      def classname: Parser[String] = repsep(ident, ".") ^^ { _.mkString(".") }
      def benches: Parser[Configuration] = listOf("benches", "b") ^^ {
        case names => Configuration(names, Persistor.None, Context.empty)
      }
      def persistor: Parser[Configuration] = "-" ~ ("persistor" | "p") ~ ident ^^ {
        case _ ~ _ ~ name => Configuration(Nil, persistorFor(name), Context.empty)
      }
      def intsetting: Parser[Configuration] = "-" ~ ident ~ decimalNumber ^^ {
        case _ ~ "Cwarmups" ~ num => Configuration(Nil, Persistor.None, Context(Key.warmupRuns -> num.toInt))
        case _ ~ "Cruns" ~ num => Configuration(Nil, Persistor.None, Context(Key.benchRuns -> num.toInt))
      }
      def flag: Parser[Configuration] = "-" ~ ident ^^ {
        case _ ~ "verbose" => Configuration(Nil, Persistor.None, Context(Key.verbose -> true))
        case _ ~ unknownFlag => sys.error(s"Unknown flag '$unknownFlag'")
      }

      parseAll(arguments, args.mkString(" ")) match {
        case Success(result, _) => result
        case Failure(msg, _) => sys.error("failed to parse args: " + msg)
        case Error(msg, _) => sys.error("error while parsing args: " + msg)
      }
    }
  }

}















