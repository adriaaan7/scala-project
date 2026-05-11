import cats.effect.{IO, IOApp}
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import sttp.tapir.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object Main extends IOApp.Simple:

  val helloEndpoint = endpoint
    .get
    .in("hello")
    .in(query[String]("name"))
    .out(stringBody)

  val helloServerEndpoint = helloEndpoint.serverLogicSuccess { name =>
    IO.pure(s"Hi $name! Welcome to test endpoint in Travel Planner.")
  }

  // Swagger config
  val swaggerEndpoints = SwaggerInterpreter()
    .fromServerEndpoints[IO](List(helloServerEndpoint), "Travel Planner API", "1.0")

  val routes = Http4sServerInterpreter[IO]().toRoutes(helloServerEndpoint :: swaggerEndpoints)

  def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(routes.orNotFound)
      .build
      .use { _ =>
        IO.println("[SWAGGER] Documentation available at http://localhost:8080/docs") *> IO.never
      }