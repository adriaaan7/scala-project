package repository

import domain.TripParticipant
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import java.util.UUID

object TripParticipantRepository:

  def save(participant: TripParticipant): ConnectionIO[TripParticipant] =
    sql"""
      INSERT INTO trip_participants (id, trip_id, user_id)
      VALUES (${participant.id}, ${participant.tripId}, ${participant.userId})
    """
      .update
      .run
      .map(_ => participant)

  def findTripIdsByUserId(userId: UUID): ConnectionIO[List[UUID]] =
    sql"""SELECT trip_id FROM trip_participants WHERE user_id = $userId"""
      .query[UUID]
      .to[List]
