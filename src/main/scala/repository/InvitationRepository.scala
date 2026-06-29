package repository

import domain.{Invitation, InvitationStatus}
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import java.util.UUID

object InvitationRepository:

  private given Meta[InvitationStatus] =
    Meta[String].timap(InvitationStatus.fromString)(_.value)

  def findById(id: UUID): ConnectionIO[Option[Invitation]] =
    sql"""SELECT id, trip_id, sender_id, receiver_id, status FROM invitations WHERE id = $id"""
      .query[(UUID, UUID, UUID, UUID, InvitationStatus)]
      .map { case (id, tripId, senderId, receiverId, status) =>
        Invitation(id, tripId, senderId, receiverId, status)
      }
      .option

  def findPendingByReceiverId(receiverId: UUID): ConnectionIO[List[(UUID, String, String)]] =
    sql"""
      SELECT i.id, t.title, u.username
      FROM invitations i
      JOIN trips t ON i.trip_id = t.id
      JOIN users u ON i.sender_id = u.id
      WHERE i.receiver_id = $receiverId AND i.status = 'pending'
    """
      .query[(UUID, String, String)]
      .to[List]

  def save(invitation: Invitation): ConnectionIO[Invitation] =
    sql"""
      INSERT INTO invitations (id, trip_id, sender_id, receiver_id, status)
      VALUES (${invitation.id}, ${invitation.tripId}, ${invitation.senderId}, ${invitation.receiverId}, ${invitation.status.value})
    """
      .update
      .run
      .map(_ => invitation)

  def updateStatus(id: UUID, status: InvitationStatus): ConnectionIO[Unit] =
    sql"""UPDATE invitations SET status = ${status.value} WHERE id = $id"""
      .update
      .run
      .map(_ => ())

  def existsByTripIdAndReceiverId(tripId: UUID, receiverId: UUID): ConnectionIO[Boolean] =
    sql"""
      SELECT COUNT(*) FROM invitations
      WHERE trip_id = $tripId AND receiver_id = $receiverId AND status = 'pending'
    """
      .query[Int]
      .unique
      .map(_ > 0)
