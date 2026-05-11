package domain

import java.util.UUID

enum InvitationStatus(val value: String):
  case Pending extends InvitationStatus("pending")
  case Accepted extends InvitationStatus("accepted")
  case Declined extends InvitationStatus("declined")

object InvitationStatus:
  def fromString(s: String): InvitationStatus =
    InvitationStatus.values.find(_.value == s.toLowerCase)
      .getOrElse(throw new IllegalArgumentException(s"Unknown status: $s"))

case class Invitation(
                       id: UUID,
                       tripId: UUID,
                       senderId: UUID,
                       receiverId: UUID,
                       status: InvitationStatus
                     )
