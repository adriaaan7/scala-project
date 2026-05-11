package domain

import java.util.UUID
import io.circe.{Codec, Encoder, Decoder}

case class User(
                 id: UUID,
                 username: String,
                 passwordHash: String
               )

object User:
  implicit val uuidEncoder: Encoder[UUID] = Encoder.encodeString.contramap(_.toString)
  implicit val uuidDecoder: Decoder[UUID] = Decoder.decodeString.emapTry(s => scala.util.Try(UUID.fromString(s)))
  implicit val codec: Codec[User] = io.circe.generic.semiauto.deriveCodec
