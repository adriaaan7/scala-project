package api

import cats.effect.IO
import cats.implicits.*
import domain.Place
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import io.circe.Codec
import io.circe.generic.semiauto.*
import repository.TripRepository
import service.{JwtService, PlaceService}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.time.OffsetDateTime
import java.util.UUID

object PlaceRoutes:

  // Request/Response DTOs
  case class CreatePlaceRequest(
    name: String,
    description: Option[String] = None,
    lat: BigDecimal,
    lng: BigDecimal,
    startDate: OffsetDateTime,
    endDate: OffsetDateTime
  )
  object CreatePlaceRequest:
    implicit val codec: Codec[CreatePlaceRequest] = deriveCodec

  case class UpdatePlaceRequest(
    name: Option[String] = None,
    description: Option[String] = None,
    lat: Option[BigDecimal] = None,
    lng: Option[BigDecimal] = None,
    startDate: Option[OffsetDateTime] = None,
    endDate: Option[OffsetDateTime] = None
  )
  object UpdatePlaceRequest:
    implicit val codec: Codec[UpdatePlaceRequest] = deriveCodec

  case class ErrorResponse(message: String)
  object ErrorResponse:
    implicit val codec: Codec[ErrorResponse] = deriveCodec
  
  private def extractUserId(authHeader: String): IO[Either[String, UUID]] =
    JwtService.getTokenFromHeader(authHeader) match
      case Right(token) =>
        JwtService.validateToken(token).map { result =>
          result.flatMap { payload =>
            try
              Right(UUID.fromString(payload.userId))
            catch
              case _: IllegalArgumentException =>
                Left(s"Invalid UUID format for userId: ${payload.userId}")
          }
        }
      case Left(error) => IO.pure(Left(error))

  // POST /trips/{tripId}/places
  def createPlaceEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .post
      .in("trips" / path[UUID]("tripId") / "places")
      .in(header[String]("Authorization"))
      .in(jsonBody[CreatePlaceRequest])
      .out(statusCode(StatusCode.Created).and(jsonBody[Place]))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Places")
      .serverLogic { case (tripId: UUID, authHeader: String, req: CreatePlaceRequest) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            TripRepository.isOwnerOrParticipant(tripId, userId).transact(xa).flatMap { hasAccess =>
              if !hasAccess then IO.pure(Left((StatusCode.Forbidden, ErrorResponse("Access denied"))))
              else
                PlaceService.createPlace(tripId, req.name, req.description, req.lat, req.lng, req.startDate, req.endDate, xa).map {
                  case Right(place) => Right(place)
                  case Left(error)  => Left((StatusCode.Conflict, ErrorResponse(error)))
                }
            }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  // PUT /trips/{tripId}/places
  private def bulkUpdatePlacesEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .put
      .in("trips" / path[UUID]("tripId") / "places")
      .in(header[String]("Authorization"))
      .in(jsonBody[List[Place]])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Places")
      .serverLogic { case (tripId: UUID, authHeader: String, newPlaces: List[Place]) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            TripRepository.isOwnerOrParticipant(tripId, userId).transact(xa).flatMap { hasAccess =>
              if !hasAccess then IO.pure(Left((StatusCode.Forbidden, ErrorResponse("Access denied"))))
              else
                PlaceService.deletePlacesByTripId(tripId, xa).flatMap { _ =>
                  newPlaces.foldM(()) { (_, place) =>
                    PlaceService.createPlace(tripId, place.name, place.description, place.lat, place.lng, place.startDate, place.endDate, xa).void
                  }.map(_ => Right(()))
                }
            }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  // PATCH /trips/{tripId}/places/{placeId}
  private def updatePlaceEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .patch
      .in("trips" / path[UUID]("tripId") / "places" / path[UUID]("placeId"))
      .in(header[String]("Authorization"))
      .in(jsonBody[UpdatePlaceRequest])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Places")
      .serverLogic { case (tripId: UUID, placeId: UUID, authHeader: String, req: UpdatePlaceRequest) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            TripRepository.isOwnerOrParticipant(tripId, userId).transact(xa).flatMap { hasAccess =>
              if !hasAccess then IO.pure(Left((StatusCode.Forbidden, ErrorResponse("Access denied"))))
              else
                PlaceService.updatePlace(placeId, req.name, req.description, req.lat, req.lng, req.startDate, req.endDate, xa).map {
                  case Right(_)    => Right(())
                  case Left(error) => Left((StatusCode.Conflict, ErrorResponse(error)))
                }
            }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  // DELETE /trips/{tripId}/places/{placeId}
  private def deletePlaceEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .delete
      .in("trips" / path[UUID]("tripId") / "places" / path[UUID]("placeId"))
      .in(header[String]("Authorization"))
      .out(statusCode(StatusCode.NoContent))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Places")
      .serverLogic { case (tripId: UUID, placeId: UUID, authHeader: String) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            TripRepository.isOwnerOrParticipant(tripId, userId).transact(xa).flatMap { hasAccess =>
              if !hasAccess then IO.pure(Left((StatusCode.Forbidden, ErrorResponse("Access denied"))))
              else PlaceService.deletePlace(placeId, xa).map(_ => Right(()))
            }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  // GET /trips/{tripId}/places
  def listPlacesByTripEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .get
      .in("trips" / path[UUID]("tripId") / "places")
      .in(header[String]("Authorization"))
      .out(jsonBody[List[Place]])
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Places")
      .serverLogic { case (tripId: UUID, authHeader: String) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            TripRepository.isOwnerOrParticipant(tripId, userId).transact(xa).flatMap { hasAccess =>
              if !hasAccess then IO.pure(Left((StatusCode.Forbidden, ErrorResponse("Access denied"))))
              else PlaceService.getPlacesByTripId(tripId, xa).map(places => Right(places))
            }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  def endpoints(xa: HikariTransactor[IO]): List[ServerEndpoint[Any, IO]] = List(
    listPlacesByTripEndpoint(xa),
    createPlaceEndpoint(xa),
    bulkUpdatePlacesEndpoint(xa),
    updatePlaceEndpoint(xa),
    deletePlaceEndpoint(xa)
  )





