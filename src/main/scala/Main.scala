import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.ember.server.EmberServerBuilder
import doobie.implicits.*

import db.Database
import api.*

object Main extends IOApp.Simple:

  def run: IO[Unit] =

    // db setup
    Database.makeTransactor.use { xa =>

      val tripEndpoints = new TripEndpoint(xa)
      val authEndpoints = new AuthEndpoint(xa)
      val placeEndpoints = new PlaceEndpoint(xa)

      val allEndpoints = authEndpoints.all ::: tripEndpoints.all ::: placeEndpoints.all

      val routes = HttpRouter.makeRoutes(allEndpoints)

      for
        _ <- EmberServerBuilder
          .default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(routes.orNotFound)
          .build
          .use(_ => IO.println("Server running at port: 8080") *> IO.never)
      yield ()
    }