package api

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import doobie.hikari.HikariTransactor
import dto.{RegisterRequest, LoginRequest, ErrorResponse}
import domain.{User}
import service.{AuthService, AuthResponse, JwtService}
import sttp.model.StatusCode

class AuthEndpoint(xa: HikariTransactor[IO]):

  private val registerEndpoint: Endpoint[Unit, RegisterRequest, (StatusCode, ErrorResponse), User, Any] =
    endpoint
      .post
      .in("auth" / "register")
      .in(jsonBody[RegisterRequest])
      .out(statusCode(StatusCode.Created).and(jsonBody[User]))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Authentication")

  private val registerServerEndpoint: ServerEndpoint[Any, IO] =
    registerEndpoint.serverLogic { req =>
      AuthService.register(req.username, req.password, xa).map {
        case Right(user) => Right(user)
        case Left(error) =>
          if error == "Username cannot be empty" || error == "Password cannot be empty" then
            Left((StatusCode.BadRequest, ErrorResponse("BAD_REQUEST", error)))
          else
            Left((StatusCode.Conflict, ErrorResponse("CONFLICT", error)))
      }
    }

  private val loginEndpoint: Endpoint[Unit, LoginRequest, (StatusCode, ErrorResponse), AuthResponse, Any] =
    endpoint
      .post
      .in("auth" / "login")
      .in(jsonBody[LoginRequest])
      .out(statusCode(StatusCode.Ok).and(jsonBody[AuthResponse]))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Authentication")

  private val loginServerEndpoint: ServerEndpoint[Any, IO] =
    loginEndpoint.serverLogic { req =>
      AuthService.login(req.username, req.password, xa).map {
        case Right(authResponse) => Right(authResponse)
        case Left(error) =>
          if error == "Username cannot be empty" || error == "Password cannot be empty" then
            Left((StatusCode.BadRequest, ErrorResponse("BAD_REQUEST", error)))
          else
            Left((StatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", error)))
      }
    }

  private val refreshEndpoint: Endpoint[Unit, Option[String], (StatusCode, ErrorResponse), AuthResponse, Any] =
    endpoint
      .post
      .in("auth" / "refresh")
      .in(header[Option[String]]("Authorization"))
      .out(statusCode(StatusCode.Ok).and(jsonBody[AuthResponse]))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Authentication")

  private val refreshServerEndpoint: ServerEndpoint[Any, IO] =
    refreshEndpoint.serverLogic { authHeaderOpt =>
      authHeaderOpt match
        case None =>
          IO.pure(Left((StatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Authorization header is required"))))
        case Some(authHeader) if authHeader.trim.isEmpty =>
          IO.pure(Left((StatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Authorization header cannot be empty"))))
        case Some(authHeader) =>
          (for
            token <- JwtService.getTokenFromHeader(authHeader) match
              case Right(t) => IO.pure(t)
              case Left(err) => IO.raiseError(new Exception(err))
            payload <- JwtService.validateToken(token).flatMap {
              case Right(p) => IO.pure(p)
              case Left(err) => IO.raiseError(new Exception(err))
            }
          yield AuthResponse(token, payload.userId, payload.username))
            .map(authResponse => Right(authResponse))
            .handleErrorWith { _ =>
              IO.pure(Left((StatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", "Invalid or expired token"))))
            }
    }

  val all: List[ServerEndpoint[Any, IO]] = List(
    registerServerEndpoint,
    loginServerEndpoint,
    refreshServerEndpoint
  )