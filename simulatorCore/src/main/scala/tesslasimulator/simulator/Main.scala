package tesslasimulator.simulator

import io.circe.syntax._

import tesslasimulator.shared.StreamJsonCodec._
import tesslasimulator.shared.ErrorJsonCodec._

object Main extends App {
  val scenarioSource =
    """
      |Events<Unit> a;
      |
      |a(3, 4, 10) = #;
    """.stripMargin

  val tesslaSource =
    """
      |in a: Events<Unit>
      |
      |define x := inPast(2, a)
      |
      |out x
    """.stripMargin

  Simulator.simulate(scenarioSource, tesslaSource) match {
    case Left(streams) => {
      println(streams.asJson.spaces2)
    }
    case Right(errors) => {
      for (error <- errors) {
        println(error.asJson.spaces2)
      }
    }
  }
}
