package dto

import io.circe.{Codec, Encoder, Decoder}
import io.circe.generic.semiauto._

case class RegisterRequest(username: String, password: String)

object RegisterRequest:
  implicit val codec: Codec[RegisterRequest] = deriveCodec

case class LoginRequest(username: String, password: String)

object LoginRequest:
  implicit val codec: Codec[LoginRequest] = deriveCodec

case class ErrorResponse(error: String, message: String)

object ErrorResponse:
  implicit val codec: Codec[ErrorResponse] = deriveCodec