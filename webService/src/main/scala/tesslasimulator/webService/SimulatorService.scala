package tesslasimulator.webService

import io.circe.{Printer, Decoder, Encoder}
import io.circe.generic.auto._
import org.http4s.HttpService
import org.http4s.circe
import org.http4s.dsl._

import tesslasimulator.simulator.Simulator
import tesslasimulator.shared.StreamJsonCodec._
import ExtendedJsonCodec._

object SimulatorService {
  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]) = circe.jsonOf[A]
  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]) = circe.jsonEncoderWithPrinterOf[A](Printer.spaces2)

  case class SimulateRequest(scenarioSpec: String, tesslaSpec: String)

  val service = HttpService {
    case GET -> Root => Ok("Hello World!")

    case req@POST -> Root / "simulate" =>
      req.decodeStrict[SimulateRequest] { request =>
        try {
          Simulator.simulate(request.scenarioSpec, request.tesslaSpec) match {
            case Left(streams) => Ok(Map("streams" -> streams))
            case Right(errors) => Ok(Map("errors" -> errors))
          }
        } catch {
          case e: Exception => InternalServerError(e.getClass.getCanonicalName)
        }
      }
  }
}
