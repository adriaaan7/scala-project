package service

import cats.effect.IO
import domain.User
import repository.UserRepository
import doobie.hikari.HikariTransactor
import doobie.implicits._
import java.util.UUID
import java.security.MessageDigest
import java.util.Base64
import io.circe.{Codec}
import io.circe.generic.semiauto._

case class AuthResponse(token: String, userId: String, username: String)

object AuthResponse:
  implicit val codec: Codec[AuthResponse] = deriveCodec

object AuthService:

  def hashPassword(password: String): String =
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(password.getBytes("UTF-8"))
    Base64.getEncoder.encodeToString(hash)

  def verifyPassword(password: String, hash: String): Boolean =
    hashPassword(password) == hash

  private def validateCredentials(username: String, password: String): Either[String, Unit] =
    if username == null || username.trim.isEmpty then
      Left("Username cannot be empty")
    else if password == null || password.trim.isEmpty then
      Left("Password cannot be empty")
    else
      Right(())

  def register(username: String, password: String, xa: HikariTransactor[IO]): IO[Either[String, User]] =
    validateCredentials(username, password) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        val user = User(
          id = UUID.randomUUID(),
          username = username,
          passwordHash = hashPassword(password)
        )

        UserRepository.findByUsername(username)
          .transact(xa)
          .flatMap { existing =>
            if existing.isDefined then
              IO.pure(Left("User already exists"))
            else
              UserRepository.save(user)
                .transact(xa)
                .map(_ => Right(user))
          }

  def login(username: String, password: String, xa: HikariTransactor[IO]): IO[Either[String, AuthResponse]] =
    validateCredentials(username, password) match
      case Left(error) => IO.pure(Left(error))
      case Right(_) =>
        UserRepository.findByUsername(username)
          .transact(xa)
          .flatMap { maybeUser =>
            maybeUser match
              case Some(user) =>
                if verifyPassword(password, user.passwordHash) then
                  val token = JwtService.generateToken(user.id.toString, user.username)
                  IO.pure(Right(AuthResponse(token, user.id.toString, user.username)))
                else
                  IO.pure(Left("Invalid credentials"))
              case None =>
                IO.pure(Left("User not found"))
          }
