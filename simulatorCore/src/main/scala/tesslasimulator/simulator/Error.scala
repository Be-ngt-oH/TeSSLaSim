package tesslasimulator.simulator

import de.uni_luebeck.isp.tessla.Parser.ParserError
import de.uni_luebeck.isp.tessla.TypeChecker.{TypeMatchError, UnknownFunctionError}
import tesslasimulator.shared.Error._
import de.uni_luebeck.isp.tessla.{Diagnostic, Function, SourceLoc}

object Error {
  case class ScenarioTesslaSpecMismatchError(msg: String)
    extends Exception(msg)
    with Error with DefaultFormatting

  case class UnexpectedArgsException(args: Seq[Any])
    extends Exception(s"Encountered unexpected arguments in simulation: $args")
      with Error with DefaultFormatting

  case class UnknownFunctionException(function: Function)
    extends Exception(s"Encountered unknown function in simulation: $function")
      with Error with DefaultFormatting

  case class TesslaDiagnostic(diagnostic: Diagnostic)
    extends Exception(diagnostic.toString)
      with Error {
    override def format = {
      diagnostic match {
        case d: ParserError => {
          val failure = d.parserFailure
          val pos = failure.loc.from
          s"${failure.message} (line ${pos.line}, column ${pos.column}): ${failure.found.string}"
        }
        case d: UnknownFunctionError => {
          s"Referenced function ${d.name} is unknown."
        }
        case d: TypeMatchError => {
          // FIXME: The type of d.atLocation changes at runtime...what's going on :o
          val pos = d.atLocation.asInstanceOf[SourceLoc].loc.from
          s"Type mismatch at (line ${pos.line}, column ${pos.column})."
        }
        case _ => diagnostic.toString
      }
    }
  }

  case class SimulationException(msg: String)
    extends Exception(msg)
      with Error with DefaultFormatting

  case class ScenarioCompilationException(e: Throwable)
    extends Exception(e)
      with Error with DefaultFormatting
}
