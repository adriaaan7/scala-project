package api

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

class UserEndpoint:

  private val helloEndpoint = endpoint
    .get
    .in("hello")
    .out(stringBody)

  private val helloServerEndpoint: ServerEndpoint[Any, IO] =
    helloEndpoint.serverLogicSuccess(_ => IO.pure("API working"))

  val all: List[ServerEndpoint[Any, IO]] = List(helloServerEndpoint)