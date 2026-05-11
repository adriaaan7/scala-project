package api

import cats.effect.IO
import org.http4s.HttpRoutes
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object HttpRouter:

  def makeRoutes(endpoints: List[ServerEndpoint[Any, IO]]): HttpRoutes[IO] =

    val swaggerEndpoints = SwaggerInterpreter()
      .fromServerEndpoints[IO](endpoints, "Travel Planner API", "1.0")

    val allEndpoints = endpoints ::: swaggerEndpoints

    Http4sServerInterpreter[IO]().toRoutes(allEndpoints)