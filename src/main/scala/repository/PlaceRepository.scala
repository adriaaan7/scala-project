package repository

import domain.Place
import java.util.UUID
import java.time.OffsetDateTime
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

object PlaceRepository:

  def findByTripId(tripId: UUID): ConnectionIO[List[Place]] =
    sql"""SELECT id, trip_id, name, description, lat, lng, start_date, end_date FROM places WHERE trip_id = $tripId"""
      .query[(UUID, UUID, String, Option[String], BigDecimal, BigDecimal, OffsetDateTime, OffsetDateTime)]
      .map { case (id, tripId, name, description, lat, lng, startDate, endDate) =>
        Place(id, tripId, name, description, lat, lng, startDate, endDate)
      }
      .to[List]

  def save(place: Place): ConnectionIO[Place] =
    sql"""INSERT INTO places (id, trip_id, name, description, lat, lng, start_date, end_date)
         VALUES (${place.id}, ${place.tripId}, ${place.name}, ${place.description}, ${place.lat}, ${place.lng}, ${place.startDate}, ${place.endDate})"""
      .update
      .run
      .map(_ => place)

  def findById(id: UUID): ConnectionIO[Option[Place]] =
    sql"""SELECT id, trip_id, name, description, lat, lng, start_date, end_date FROM places WHERE id = $id"""
      .query[(UUID, UUID, String, Option[String], BigDecimal, BigDecimal, OffsetDateTime, OffsetDateTime)]
      .map { case (id, tripId, name, description, lat, lng, startDate, endDate) =>
        Place(id, tripId, name, description, lat, lng, startDate, endDate)
      }
      .option

  def update(place: Place): ConnectionIO[Unit] =
    sql"""UPDATE places
         SET trip_id = ${place.tripId},
             name = ${place.name},
             description = ${place.description},
             lat = ${place.lat},
             lng = ${place.lng},
             start_date = ${place.startDate},
             end_date = ${place.endDate}
         WHERE id = ${place.id}"""
      .update
      .run
      .map(_ => ())

  def delete(id: UUID): ConnectionIO[Unit] =
    sql"""DELETE FROM places WHERE id = $id"""
      .update
      .run
      .map(_ => ())

  def deleteByTripId(tripId: UUID): ConnectionIO[Unit] =
    sql"""DELETE FROM places WHERE trip_id = $tripId"""
      .update
      .run
      .map(_ => ())


