package api

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint
import doobie.hikari.HikariTransactor

class TripEndpoint(xa: HikariTransactor[IO]):
  val all: List[ServerEndpoint[Any, IO]] = TripRoutes.endpoints(xa)


