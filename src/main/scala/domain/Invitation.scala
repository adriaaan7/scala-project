package domain

import java.util.UUID
import io.circe.{Codec, Encoder, Decoder}
import io.circe.generic.semiauto.*

enum InvitationStatus(val value: String):
  case Pending extends InvitationStatus("pending")
  case Accepted extends InvitationStatus("accepted")
  case Declined extends InvitationStatus("declined")

object InvitationStatus:
  def fromString(s: String): InvitationStatus =
    InvitationStatus.values.find(_.value == s.toLowerCase)
      .getOrElse(throw new IllegalArgumentException(s"Unknown status: $s"))

  implicit val encoder: Encoder[InvitationStatus] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[InvitationStatus] = Decoder.decodeString.emapTry(s => scala.util.Try(fromString(s)))

case class Invitation(
                       id: UUID,
                       tripId: UUID,
                       senderId: UUID,
                       receiverId: UUID,
                       status: InvitationStatus
                     )

object Invitation:
  implicit val codec: Codec[Invitation] = deriveCodec
