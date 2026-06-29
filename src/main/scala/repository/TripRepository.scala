package repository

import domain.Trip
import java.util.UUID
import java.time.LocalDate
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import cats.effect.IO

object TripRepository:

  def findById(id: UUID): ConnectionIO[Option[Trip]] =
    sql"""SELECT id, title, start_date, end_date, owner_id FROM trips WHERE id = $id"""
      .query[(UUID, String, LocalDate, LocalDate, UUID)]
      .map { case (id, title, startDate, endDate, ownerId) =>
        Trip(id, title, startDate, endDate, ownerId)
      }
      .option

  def listAll: ConnectionIO[List[Trip]] =
    sql"""SELECT id, title, start_date, end_date, owner_id FROM trips"""
      .query[(UUID, String, LocalDate, LocalDate, UUID)]
      .map { case (id, title, startDate, endDate, ownerId) =>
        Trip(id, title, startDate, endDate, ownerId)
      }
      .to[List]

  def findByOwnerId(ownerId: UUID): ConnectionIO[List[Trip]] =
    sql"""SELECT id, title, start_date, end_date, owner_id FROM trips WHERE owner_id = $ownerId"""
      .query[(UUID, String, LocalDate, LocalDate, UUID)]
      .map { case (id, title, startDate, endDate, ownerId) =>
        Trip(id, title, startDate, endDate, ownerId)
      }
      .to[List]

  def save(trip: Trip): ConnectionIO[Trip] =
    sql"""INSERT INTO trips (id, title, start_date, end_date, owner_id)
         VALUES (${trip.id}, ${trip.title}, ${trip.startDate}, ${trip.endDate}, ${trip.ownerId})"""
      .update
      .run
      .map(_ => trip)

  def update(trip: Trip): ConnectionIO[Unit] =
    sql"""UPDATE trips
         SET title = ${trip.title},
             start_date = ${trip.startDate},
             end_date = ${trip.endDate}
         WHERE id = ${trip.id}"""
      .update
      .run
      .map(_ => ())

  def delete(id: UUID): ConnectionIO[Unit] =
    sql"""DELETE FROM trips WHERE id = $id"""
      .update
      .run
      .map(_ => ())

  def findByOwnerOrParticipant(userId: UUID): ConnectionIO[List[Trip]] =
    sql"""
      SELECT id, title, start_date, end_date, owner_id FROM trips
      WHERE owner_id = $userId
         OR id IN (SELECT trip_id FROM trip_participants WHERE user_id = $userId)
    """
      .query[(UUID, String, LocalDate, LocalDate, UUID)]
      .map { case (id, title, startDate, endDate, ownerId) =>
        Trip(id, title, startDate, endDate, ownerId)
      }
      .to[List]

