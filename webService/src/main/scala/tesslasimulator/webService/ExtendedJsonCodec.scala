package tesslasimulator.webService

import de.uni_luebeck.isp.compacom.{Location, Position}

import scala.language.implicitConversions
import io.circe._
import io.circe.syntax._
import de.uni_luebeck.isp.tessla.Parser.ParserError
import de.uni_luebeck.isp.tessla.SourceLoc
import de.uni_luebeck.isp.tessla.TypeChecker.TypeMatchError
import org.parboiled2
import tesslasimulator.parser.Error.ParsingError
import tesslasimulator.shared.Error.Error
import tesslasimulator.simulator.Error.ScenarioTesslaSpecMismatchError

object ExtendedJsonCodec {
  import tesslasimulator.simulator.Error.TesslaDiagnostic

  implicit def stringToJson(s: String): Json = s.asJson

  implicit val encodeScenarioParsingError: Encoder[ParsingError] = new Encoder[ParsingError] {
    implicit val encodePosition: Encoder[parboiled2.Position] =
      Encoder.forProduct2("line", "column")(p => (p.line, p.column))

    final def apply(p: ParsingError): Json = {
      Json.fromFields(Map(
        "type" -> "Scenario",
        "subtype" -> p.getClass.getSimpleName,
        "message" -> p.format,
        "details" -> Map("from" -> p.parseError.principalPosition).asJson
      ))
    }
  }

  implicit val encodeTesslaDiagnostic: Encoder[TesslaDiagnostic] = new Encoder[TesslaDiagnostic] {
    implicit val encodeTesslaParserLocation: Encoder[Location] = new Encoder[Location] {
      implicit val encodePosition: Encoder[Position] =
        Encoder.forProduct2("line", "column")(p => (p.line, p.column))

      final def apply(loc: Location): Json = Json.fromFields(Map(
        "from" -> loc.from.asJson,
        "to" -> loc.to.asJson
      ))
    }

    final def apply(e: TesslaDiagnostic): Json = {
      val base = Json.fromFields(Map(
        "type" -> "TeSSLa",
        "subtype" -> e.diagnostic.getClass.getSimpleName,
        "message" -> e.format
      ))

      e.diagnostic match {
        case p: ParserError => base.deepMerge(Json.fromFields(Map(
          "details" -> p.parserFailure.loc.asJson
        )))
        case t: TypeMatchError => base.deepMerge(Json.fromFields(Map(
          // FIXME: The type of d.atLocation changes at runtime...what's going on :o
          "details" -> t.atLocation.asInstanceOf[SourceLoc].loc.asJson
        )))
        case _ => base
      }
    }
  }

  implicit val encodeSTSME: Encoder[ScenarioTesslaSpecMismatchError] =
    // TODO: Provide more detail
    Encoder.forProduct3("type", "subtype", "message")(e => ("TeSSLa", "ScenarioTesslaSpecMismatch", e.format))

  implicit val encodeError: Encoder[Error] = new Encoder[Error] {
    final def apply(e: Error): Json = e match {
      case e: ParsingError => encodeScenarioParsingError(e)
      case e: TesslaDiagnostic => encodeTesslaDiagnostic(e)
      case e: ScenarioTesslaSpecMismatchError => encodeSTSME(e)
      case e => Json.fromFields(Map(
        "type" -> "Scenario",
        "subtype" -> e.getClass.getSimpleName,
        "message" -> e.format
      ))
    }
  }
}
