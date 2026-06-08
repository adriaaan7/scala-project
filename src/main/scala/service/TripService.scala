package service

import cats.effect.IO
import domain.Trip
import repository.TripRepository
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import java.util.UUID
import java.time.LocalDate

object TripService:

  def listAllTrips(xa: HikariTransactor[IO]): IO[List[Trip]] =
    TripRepository.listAll.transact(xa)

  def listTripsByUserId(userId: UUID, xa: HikariTransactor[IO]): IO[List[Trip]] =
    TripRepository.findByOwnerId(userId).transact(xa)

  def createTrip(title: String, startDate: LocalDate, endDate: LocalDate, ownerId: UUID, xa: HikariTransactor[IO]): IO[Trip] =
    val trip = Trip(UUID.randomUUID(), title, startDate, endDate, ownerId)
    TripRepository.save(trip).transact(xa)

  def getTripById(id: UUID, xa: HikariTransactor[IO]): IO[Option[Trip]] =
    TripRepository.findById(id).transact(xa)

  def updateTrip(id: UUID, title: Option[String], startDate: Option[LocalDate], endDate: Option[LocalDate], xa: HikariTransactor[IO]): IO[Unit] =
    TripRepository.findById(id).transact(xa).flatMap {
      case Some(trip) =>
        val updated = trip.copy(
          title = title.getOrElse(trip.title),
          startDate = startDate.getOrElse(trip.startDate),
          endDate = endDate.getOrElse(trip.endDate)
        )
        TripRepository.update(updated).transact(xa)
      case None => IO.unit
    }

  def deleteTrip(id: UUID, xa: HikariTransactor[IO]): IO[Unit] =
    TripRepository.delete(id).transact(xa)

