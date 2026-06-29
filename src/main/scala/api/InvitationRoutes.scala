package api

import cats.effect.IO
import domain.InvitationStatus
import doobie.hikari.HikariTransactor
import io.circe.Codec
import io.circe.generic.semiauto.*
import service.{InvitationService, JwtService}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.util.UUID

object InvitationRoutes:

  case class InviteRequest(username: String)
  object InviteRequest:
    implicit val codec: Codec[InviteRequest] = deriveCodec

  case class InvitationStatusRequest(status: String)
  object InvitationStatusRequest:
    implicit val codec: Codec[InvitationStatusRequest] = deriveCodec

  case class InvitationListItem(id: UUID, title: String, senderUsername: String)
  object InvitationListItem:
    implicit val codec: Codec[InvitationListItem] = deriveCodec

  case class ErrorResponse(message: String)
  object ErrorResponse:
    implicit val codec: Codec[ErrorResponse] = deriveCodec

  private def extractUserId(authHeader: String): IO[Either[String, UUID]] =
    JwtService.getTokenFromHeader(authHeader) match
      case Right(token) =>
        JwtService.validateToken(token).map { result =>
          result.flatMap { payload =>
            try Right(UUID.fromString(payload.userId))
            catch case _: IllegalArgumentException =>
              Left(s"Invalid UUID format for userId: ${payload.userId}")
          }
        }
      case Left(error) => IO.pure(Left(error))

  def inviteUserEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .post
      .in("trips" / path[UUID]("tripId") / "invite")
      .in(header[String]("Authorization"))
      .in(jsonBody[InviteRequest])
      .out(statusCode(StatusCode.Created))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Collaboration")
      .serverLogic { case (tripId: UUID, authHeader: String, req: InviteRequest) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            InvitationService.sendInvitation(tripId, userId, req.username, xa).map {
              case Right(_) => Right(())
              case Left(error) if error.contains("not found") || error.contains("not exist") =>
                Left((StatusCode.NotFound, ErrorResponse(error)))
              case Left(error) if error.contains("Only the trip owner") =>
                Left((StatusCode.Forbidden, ErrorResponse(error)))
              case Left(error) =>
                Left((StatusCode.Conflict, ErrorResponse(error)))
            }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  def listInvitationsEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .get
      .in("invitations")
      .in(header[String]("Authorization"))
      .out(jsonBody[List[InvitationListItem]])
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Collaboration")
      .serverLogic { (authHeader: String) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            InvitationService.listPendingInvitations(userId, xa).map { items =>
              Right(items.map(i => InvitationListItem(i.id, i.title, i.senderUsername)))
            }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  def respondToInvitationEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .patch
      .in("invitations" / path[UUID]("invitationId"))
      .in(header[String]("Authorization"))
      .in(jsonBody[InvitationStatusRequest])
      .out(statusCode(StatusCode.NoContent))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Collaboration")
      .serverLogic { case (invitationId: UUID, authHeader: String, req: InvitationStatusRequest) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            val statusResult =
              req.status.toLowerCase match
                case "accepted" => Right(InvitationStatus.Accepted)
                case "declined" => Right(InvitationStatus.Declined)
                case _          => Left(s"Invalid status '${req.status}'. Must be 'accepted' or 'declined'")
            statusResult match
              case Left(err) =>
                IO.pure(Left((StatusCode.BadRequest, ErrorResponse(err))))
              case Right(status) =>
                InvitationService.respondToInvitation(invitationId, status, userId, xa).map {
                  case Right(_)    => Right(())
                  case Left(error) if error.contains("Not authorized") =>
                    Left((StatusCode.Forbidden, ErrorResponse(error)))
                  case Left(error) =>
                    Left((StatusCode.NotFound, ErrorResponse(error)))
                }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  def endpoints(xa: HikariTransactor[IO]): List[ServerEndpoint[Any, IO]] = List(
    inviteUserEndpoint(xa),
    listInvitationsEndpoint(xa),
    respondToInvitationEndpoint(xa)
  )
