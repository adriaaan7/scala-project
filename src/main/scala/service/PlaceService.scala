package service

import cats.effect.IO
import domain.Place
import repository.{PlaceRepository, TripRepository}
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import java.util.UUID
import java.time.{LocalDate, OffsetDateTime}

object PlaceService:

  private def overlaps(newStart: OffsetDateTime, newEnd: OffsetDateTime, existing: List[Place], excludeId: Option[UUID] = None): Boolean =
    existing
      .filter(p => excludeId.forall(_ != p.id))
      .exists(p => newStart.isBefore(p.endDate) && p.startDate.isBefore(newEnd))

  private def withinTripBounds(placeStart: OffsetDateTime, placeEnd: OffsetDateTime, tripStart: LocalDate, tripEnd: LocalDate): Boolean =
    !placeStart.toLocalDate.isBefore(tripStart) && !placeEnd.toLocalDate.isAfter(tripEnd)

  def getPlacesByTripId(tripId: UUID, xa: HikariTransactor[IO]): IO[List[Place]] =
    PlaceRepository.findByTripId(tripId).transact(xa)

  def createPlace(tripId: UUID, name: String, description: Option[String], lat: BigDecimal, lng: BigDecimal, startDate: OffsetDateTime, endDate: OffsetDateTime, xa: HikariTransactor[IO]): IO[Either[String, Place]] =
    TripRepository.findById(tripId).transact(xa).flatMap {
      case None => IO.pure(Left("Trip not found"))
      case Some(trip) =>
        if !withinTripBounds(startDate, endDate, trip.startDate, trip.endDate) then
          IO.pure(Left(s"Place dates must fall within the trip's date range (${trip.startDate} – ${trip.endDate})"))
        else
          PlaceRepository.findByTripId(tripId).transact(xa).flatMap { existing =>
            if overlaps(startDate, endDate, existing) then
              IO.pure(Left("This place's time window overlaps with an existing place in the trip"))
            else
              val place = Place(UUID.randomUUID(), tripId, name, description, lat, lng, startDate, endDate)
              PlaceRepository.save(place).transact(xa).map(Right(_))
          }
    }

  def getPlaceById(id: UUID, xa: HikariTransactor[IO]): IO[Option[Place]] =
    PlaceRepository.findById(id).transact(xa)

  def updatePlace(id: UUID, name: Option[String], description: Option[String], lat: Option[BigDecimal], lng: Option[BigDecimal], startDate: Option[OffsetDateTime], endDate: Option[OffsetDateTime], xa: HikariTransactor[IO]): IO[Either[String, Unit]] =
    PlaceRepository.findById(id).transact(xa).flatMap {
      case None => IO.pure(Right(()))
      case Some(place) =>
        val updated = place.copy(
          name = name.getOrElse(place.name),
          description = description.orElse(place.description),
          lat = lat.getOrElse(place.lat),
          lng = lng.getOrElse(place.lng),
          startDate = startDate.getOrElse(place.startDate),
          endDate = endDate.getOrElse(place.endDate)
        )
        TripRepository.findById(place.tripId).transact(xa).flatMap {
          case None => IO.pure(Right(()))
          case Some(trip) =>
            if !withinTripBounds(updated.startDate, updated.endDate, trip.startDate, trip.endDate) then
              IO.pure(Left(s"Place dates must fall within the trip's date range (${trip.startDate} – ${trip.endDate})"))
            else
              PlaceRepository.findByTripId(place.tripId).transact(xa).flatMap { existing =>
                if overlaps(updated.startDate, updated.endDate, existing, excludeId = Some(id)) then
                  IO.pure(Left("This place's time window overlaps with an existing place in the trip"))
                else
                  PlaceRepository.update(updated).transact(xa).map(Right(_))
              }
        }
    }

  def deletePlace(id: UUID, xa: HikariTransactor[IO]): IO[Unit] =
    PlaceRepository.delete(id).transact(xa)

  def deletePlacesByTripId(tripId: UUID, xa: HikariTransactor[IO]): IO[Unit] =
    PlaceRepository.deleteByTripId(tripId).transact(xa)

