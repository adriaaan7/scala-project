package api

import cats.effect.IO
import sttp.tapir.server.ServerEndpoint
import doobie.hikari.HikariTransactor

class PlaceEndpoint(xa: HikariTransactor[IO]):
  val all: List[ServerEndpoint[Any, IO]] = PlaceRoutes.endpoints(xa)

