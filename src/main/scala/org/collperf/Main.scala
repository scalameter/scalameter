package org.collperf



import collection._
import util.parsing.combinator._



object Main {

  def main(args: Array[String]) {
    // initialize
    // prepare top-level context
    // identify test objects
    // create reporters and persistors
    val configuration = Configuration.fromCommandLineArgs(args)
    import configuration._

    // schedule benchmarks
    for (bench <- benches) Class.forName(bench).newInstance

    // execute all benchmark objects
    for (benchmark <- Runner.flushSchedule()) {
      // execute tests
      val result: Result = Runner.run(benchmark)

      // generate reports
      for (r <- reporters) r.report(result, persistor)
    }
  }

  case class Configuration(benches: Seq[String], reporters: Seq[Reporter], persistor: Persistor)

  object Configuration extends JavaTokenParsers {
    private def reporterFor(name: String) = name match {
      case "None" => Reporter.None
    }

    private def persistorFor(name: String) = name match {
      case "None" => Persistor.None
    }

    def fromCommandLineArgs(args: Array[String]) = {
      def arguments: Parser[Configuration] = rep(arg) ^^ {
        case configs => configs.foldLeft(Configuration(Nil, Nil, Persistor.None)) {
          case (acc, x) => Configuration(acc.benches ++ x.benches, acc.reporters ++ x.reporters, x.persistor)
        }
      }
      def arg: Parser[Configuration] = benches | reporters | persistor | flag
      def listOf(flagname: String, shorthand: String): Parser[Seq[String]] = "-" ~ (flagname | shorthand) ~ classnames ^^ {
        case _ ~ _ ~ classnames => classnames
      }
      def classnames: Parser[Seq[String]] = repsep(classname, ":")
      def classname: Parser[String] = repsep(ident, ".") ^^ { _.mkString(".") }
      def benches: Parser[Configuration] = listOf("benches", "b") ^^ {
        case names => Configuration(names, Nil, Persistor.None)
      }
      def reporters: Parser[Configuration] = listOf("reporters", "r") ^^ {
        case names => Configuration(Nil, names map reporterFor, Persistor.None)
      }
      def persistor: Parser[Configuration] = "-" ~ ("persistor" | "p") ~ ident ^^ {
        case _ ~ _ ~ name => Configuration(Nil, Nil, persistorFor(name))
      }
      def flag: Parser[Configuration] = "-" ~ ident ^^ {
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















