package service

import cats.effect.IO
import cats.implicits.*
import domain.{Invitation, InvitationStatus, TripParticipant}
import repository.{InvitationRepository, TripParticipantRepository, TripRepository, UserRepository}
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import java.util.UUID

object InvitationService:

  case class InvitationListItem(id: UUID, title: String, senderUsername: String)

  def sendInvitation(tripId: UUID, senderUserId: UUID, receiverUsername: String, xa: HikariTransactor[IO]): IO[Either[String, Invitation]] =
    TripRepository.findById(tripId).transact(xa).flatMap {
      case None =>
        IO.pure(Left("Trip not found"))
      case Some(trip) if trip.ownerId != senderUserId =>
        IO.pure(Left("Only the trip owner can send invitations"))
      case Some(_) =>
        UserRepository.findByUsername(receiverUsername).transact(xa).flatMap {
          case None =>
            IO.pure(Left("User not found"))
          case Some(receiver) =>
            InvitationRepository.existsByTripIdAndReceiverId(tripId, receiver.id).transact(xa).flatMap {
              case true =>
                IO.pure(Left("Invitation already sent to this user"))
              case false =>
                val invitation = Invitation(UUID.randomUUID(), tripId, senderUserId, receiver.id, InvitationStatus.Pending)
                InvitationRepository.save(invitation).transact(xa).map(inv => Right(inv))
            }
        }
    }

  def listPendingInvitations(receiverId: UUID, xa: HikariTransactor[IO]): IO[List[InvitationListItem]] =
    InvitationRepository.findPendingByReceiverId(receiverId).transact(xa)
      .map(_.map { case (id, title, senderUsername) => InvitationListItem(id, title, senderUsername) })

  def respondToInvitation(invitationId: UUID, status: InvitationStatus, respondingUserId: UUID, xa: HikariTransactor[IO]): IO[Either[String, Unit]] =
    InvitationRepository.findById(invitationId).transact(xa).flatMap {
      case None =>
        IO.pure(Left("Invitation not found"))
      case Some(inv) if inv.receiverId != respondingUserId =>
        IO.pure(Left("Not authorized to respond to this invitation"))
      case Some(inv) =>
        val updateOp: ConnectionIO[Unit] = for
          _ <- InvitationRepository.updateStatus(invitationId, status)
          _ <- if status == InvitationStatus.Accepted then
                 TripParticipantRepository.save(TripParticipant(UUID.randomUUID(), inv.tripId, respondingUserId)).void
                  else
                 ().pure[ConnectionIO]
        yield ()
        updateOp.transact(xa).map(_ => Right(()))
    }
