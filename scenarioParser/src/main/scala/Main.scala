import java.io.{File, FileWriter}

import io.circe.syntax._

import scala.io
import scala.io.Source

import tesslasimulator.parser._
import tesslasimulator.shared.StreamJsonCodec._

object Main extends App {

  case class Config(in: Option[File] = None, out: Option[File] = None)

  val cliParser = new scopt.OptionParser[Config]("Scenario DSL for TeSSLa") {
    opt[File]('f', "input file")
      .optional()
      .valueName("<filename>")
      .action((x, c) => c.copy(in = Some(x)))
      .text("(optional) file to read scenario from")

    opt[File]('o', "output file")
      .optional()
      .valueName("<filename>")
      .action((x, c) => c.copy(out = Some(x)))
      .text("(optional) file to write compiled scenario to")

    help("help").text("prints this usage text")

    note("\nIf no files are provided in-/output will be written to stdin/stdout.")
  }

  val config = cliParser.parse(args, Config()).get

  val input = (config.in match {
    case Some(file) => Source.fromFile(config.in.get)
    case None => {
      println("""Reading from STDIN. Enter "---" on its own line to mark EOI.""")
      Iterator.continually(io.StdIn.readLine).takeWhile(_ != "---")
    }
  }).mkString

  val scenarioAst = ScenarioParser.parseScenario(input) match {
    case Left(a) => a
    case Right(parsingErrors) => {
      for (error <- parsingErrors)
        println(error.format)
      sys.exit(1)
    }
  }

  TypeChecker.checkTypes(scenarioAst) match {
    case Seq() => // No type errors
    case typeErrors => {
      for (error <- typeErrors)
        println(error.format)
      sys.exit(1)
    }
  }

  val result = ScenarioCompiler.compileScenarioDescription(scenarioAst)

  val jsonOutput = result.asJson.spaces2
  config.out match {
    case Some(file) => {
      val out = new FileWriter(file)
      out.write(jsonOutput)
      out.close
    }
    case None =>
      println(jsonOutput)
  }
}