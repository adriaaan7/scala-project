package api

import domain.Trip
import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import java.util.UUID
import java.time.LocalDate
import io.circe.Codec
import io.circe.generic.semiauto.*
import sttp.model.StatusCode
import service.{TripService, JwtService, PlaceService}
import domain.Place
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.syntax.connectionio.toConnectionIOOps

object TripRoutes:

  // Request/Response DTOs
  case class CreateTripRequest(title: String, startDate: LocalDate, endDate: LocalDate)
  object CreateTripRequest:
    implicit val codec: Codec[CreateTripRequest] = deriveCodec

  case class UpdateTripRequest(title: Option[String] = None, startDate: Option[LocalDate] = None, endDate: Option[LocalDate] = None)
  object UpdateTripRequest:
    implicit val codec: Codec[UpdateTripRequest] = deriveCodec

  case class ErrorResponse(message: String)
  object ErrorResponse:
    implicit val codec: Codec[ErrorResponse] = deriveCodec

  case class TripDetails(
    id: UUID,
    title: String,
    startDate: LocalDate,
    endDate: LocalDate,
    ownerId: UUID,
    isOwner: Boolean,
    places: List[Place]
  )
  object TripDetails:
    implicit val codec: Codec[TripDetails] = deriveCodec

  // Helper function to extract userId from Authorization header
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

  // GET /trips - List all accessible trips
  def listTripsEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .get
      .in("trips")
      .in(header[String]("Authorization"))
      .out(jsonBody[List[Trip]])
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Trips")
      .serverLogic { (authHeader: String) =>
        extractUserId(authHeader).flatMap {
          case Right(_) =>
            TripService.listAllTrips(xa).map(trips => Right(trips))
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  // POST /trips - Create a new trip
  def createTripEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .post
      .in("trips")
      .in(header[String]("Authorization"))
      .in(jsonBody[CreateTripRequest])
      .out(statusCode(StatusCode.Created).and(jsonBody[Trip]))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Trips")
      .serverLogic { case (authHeader: String, req: CreateTripRequest) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            TripService.createTrip(req.title, req.startDate, req.endDate, userId, xa)
              .map(trip => Right(trip))
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  // GET /trips/{tripId} - Get full trip details
  def getTripDetailsEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .get
      .in("trips" / path[UUID]("tripId"))
      .in(header[String]("Authorization"))
      .out(jsonBody[TripDetails])
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Trips")
      .serverLogic { case (tripId: UUID, authHeader: String) =>
        extractUserId(authHeader).flatMap {
          case Right(userId) =>
            TripService.getTripById(tripId, xa).flatMap {
              case Some(trip) =>
                PlaceService.getPlacesByTripId(tripId, xa).map { places =>
                  val tripDetails = TripDetails(
                    id = trip.id,
                    title = trip.title,
                    startDate = trip.startDate,
                    endDate = trip.endDate,
                    ownerId = trip.ownerId,
                    isOwner = userId == trip.ownerId,
                    places = places
                  )
                  Right(tripDetails)
                }
              case None =>
                IO.pure(Left((StatusCode.NotFound, ErrorResponse("Trip not found"))))
            }
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  // PATCH /trips/{tripId} - Update trip metadata
  def updateTripEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .patch
      .in("trips" / path[UUID]("tripId"))
      .in(header[String]("Authorization"))
      .in(jsonBody[UpdateTripRequest])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Trips")
      .serverLogic { case (tripId: UUID, authHeader: String, req: UpdateTripRequest) =>
        extractUserId(authHeader).flatMap {
          case Right(_) =>
            TripService.updateTrip(tripId, req.title, req.startDate, req.endDate, xa)
              .map(_ => Right(()))
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  // DELETE /trips/{tripId} - Delete trip
  def deleteTripEndpoint(xa: HikariTransactor[IO]): ServerEndpoint[Any, IO] =
    endpoint
      .delete
      .in("trips" / path[UUID]("tripId"))
      .in(header[String]("Authorization"))
      .out(statusCode(StatusCode.NoContent))
      .errorOut(statusCode.and(jsonBody[ErrorResponse]))
      .tag("Trips")
      .serverLogic { case (tripId: UUID, authHeader: String) =>
        extractUserId(authHeader).flatMap {
          case Right(_) =>
            TripService.deleteTrip(tripId, xa)
              .map(_ => Right(()))
          case Left(error) =>
            IO.pure(Left((StatusCode.Unauthorized, ErrorResponse(error))))
        }
      }

  def endpoints(xa: HikariTransactor[IO]): List[ServerEndpoint[Any, IO]] = List(
    listTripsEndpoint(xa),
    createTripEndpoint(xa),
    getTripDetailsEndpoint(xa),
    updateTripEndpoint(xa),
    deleteTripEndpoint(xa)
  )