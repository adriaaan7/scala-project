package service

import cats.effect.IO
import domain.Place
import repository.PlaceRepository
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import java.util.UUID
import java.time.OffsetDateTime

object PlaceService:

  def getPlacesByTripId(tripId: UUID, xa: HikariTransactor[IO]): IO[List[Place]] =
    PlaceRepository.findByTripId(tripId).transact(xa)

  def createPlace(tripId: UUID, name: String, description: Option[String], lat: BigDecimal, lng: BigDecimal, startDate: OffsetDateTime, endDate: OffsetDateTime, xa: HikariTransactor[IO]): IO[Place] =
    val place = Place(UUID.randomUUID(), tripId, name, description, lat, lng, startDate, endDate)
    PlaceRepository.save(place).transact(xa)

  def getPlaceById(id: UUID, xa: HikariTransactor[IO]): IO[Option[Place]] =
    PlaceRepository.findById(id).transact(xa)

  def updatePlace(id: UUID, name: Option[String], description: Option[String], lat: Option[BigDecimal], lng: Option[BigDecimal], startDate: Option[OffsetDateTime], endDate: Option[OffsetDateTime], xa: HikariTransactor[IO]): IO[Unit] =
    PlaceRepository.findById(id).transact(xa).flatMap {
      case Some(place) =>
        val updated = place.copy(
          name = name.getOrElse(place.name),
          description = description.orElse(place.description),
          lat = lat.getOrElse(place.lat),
          lng = lng.getOrElse(place.lng),
          startDate = startDate.getOrElse(place.startDate),
          endDate = endDate.getOrElse(place.endDate)
        )
        PlaceRepository.update(updated).transact(xa)
      case None => IO.unit
    }

  def deletePlace(id: UUID, xa: HikariTransactor[IO]): IO[Unit] =
    PlaceRepository.delete(id).transact(xa)

  def deletePlacesByTripId(tripId: UUID, xa: HikariTransactor[IO]): IO[Unit] =
    PlaceRepository.deleteByTripId(tripId).transact(xa)

