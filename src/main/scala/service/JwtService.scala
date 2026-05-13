package service

import cats.effect.IO
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto._
import java.time.Clock

object JwtService:

  private val secretKey = System.getenv().getOrDefault("JWT_SECRET", "some secret")
  private val algorithm = JwtAlgorithm.HS256
  implicit val clock: Clock = Clock.systemUTC

  case class JwtPayload(userId: String, username: String)

  object JwtPayload:
    implicit val codec: Codec[JwtPayload] = deriveCodec

  def generateToken(userId: String, username: String, expirationSeconds: Long = 3600): String =
    val now = System.currentTimeMillis / 1000
    val payload = JwtPayload(userId, username)
    val claim = JwtClaim(
      subject = Some(userId),
      expiration = Some(now + expirationSeconds),
      issuedAt = Some(now)
    ).withContent(payload.asJson.noSpaces)

    Jwt.encode(claim, secretKey, algorithm)

  def validateToken(token: String): IO[Either[String, JwtPayload]] =
    IO.pure {
      Jwt.decode(token, secretKey, Seq(algorithm))
        .toEither
        .left.map(_.getMessage)
        .flatMap { claim =>
          io.circe.parser.parse(claim.content) match
            case Right(json) =>
              json.as[JwtPayload]
                .left.map(_.getMessage)
            case Left(err) =>
              Left(s"Failed to parse token content: ${err.getMessage}")
        }
    }

  def getTokenFromHeader(authHeader: String): Either[String, String] =
    authHeader match
      case s"Bearer $token" => Right(token.trim)
      case _ => Left("Invalid authorization header format. Expected: Bearer <token>")