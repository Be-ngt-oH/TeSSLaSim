package tesslasimulator.webService

import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.{CORS, GZip}
import org.http4s.server.{Server, ServerApp}

import scalaz.concurrent.Task

object Main extends ServerApp {
  case class Config(port: Int = 8080, hostname: String = "localhost")

  override def server(args: List[String]): Task[Server] = {
    val cliParser = new scopt.OptionParser[Config]("TeSSLa Simulator Server") {
      opt[Int]('p', "port")
        .optional()
        .valueName("<port>")
        .action((x, c) => c.copy(port = x))
        .text("port to bind to - defaults to 8080")

      opt[String]('h', "hostname")
        .optional()
        .valueName("<hostname>")
        .action((x, c) => c.copy(hostname = x))
        .text("hostname to bind to - defaults to localhost")

      help("help").text("prints this usage text")
    }

    val config = cliParser.parse(args, Config()).get

    BlazeBuilder
      .bindHttp(config.port, config.hostname)
      .mountService(GZip(CORS(SimulatorService.service)))
      .start
  }
}
