package api

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.server.middleware.CORS
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object HttpRouter:

  def makeRoutes(endpoints: List[ServerEndpoint[Any, IO]]): HttpRoutes[IO] =

    val swaggerEndpoints = SwaggerInterpreter()
      .fromServerEndpoints[IO](endpoints, "Travel Planner API", "1.0")

    val allEndpoints = endpoints ::: swaggerEndpoints

    val routes = Http4sServerInterpreter[IO]().toRoutes(allEndpoints)

    // Apply CORS middleware to allow requests from UI
    CORS.policy(routes)
