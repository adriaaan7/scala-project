package api

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import doobie.hikari.HikariTransactor

class UserEndpoint(xa: HikariTransactor[IO]):

  private val helloEndpoint = endpoint
    .get
    .in("hello")
    .out(stringBody)

  private val helloServerEndpoint: ServerEndpoint[Any, IO] =
    helloEndpoint.serverLogicSuccess(_ => IO.pure("API working"))

  private val userEndpoints: List[ServerEndpoint[Any, IO]] =
    UserRoutes.endpoints(xa)

  val all: List[ServerEndpoint[Any, IO]] = List(helloServerEndpoint) ::: userEndpoints
