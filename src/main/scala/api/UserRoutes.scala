package api

import domain.User
import repository.UserRepository
import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import doobie.implicits.*
import doobie.hikari.HikariTransactor
import java.util.UUID
import io.circe.generic.auto.*

object UserRoutes:

  case class CreateUserRequest(username: String, password: String)

  object CreateUserRequest:
    import io.circe.generic.semiauto.*
    import io.circe.Codec
    implicit val codec: Codec[CreateUserRequest] = deriveCodec[CreateUserRequest]

  def createUserEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .post
      .in("users")
      .in(jsonBody[CreateUserRequest])
      .out(jsonBody[User])
      .serverLogicSuccess { input =>
        val newUser = User(
          id = UUID.randomUUID(),
          username = input.username,
          passwordHash = input.password
        )
        for {
          _ <- UserRepository.save(newUser).transact(xa)
        } yield newUser
      }

  def endpoints(xa: HikariTransactor[IO]): List[ServerEndpoint[Any, IO]] =
    List(createUserEndpoint(xa))
