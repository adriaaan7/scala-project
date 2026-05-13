package repository

import domain.User
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.meta.*
import cats.effect.IO
import java.util.UUID

object UserRepository:

  def save(user: User): ConnectionIO[Int] =
    sql"""
      INSERT INTO users (id, username, password_hash)
      VALUES (${user.id}, ${user.username}, ${user.passwordHash})
    """.update.run

  def findByUsername(username: String): ConnectionIO[Option[User]] =
    sql"""
      SELECT id, username, password_hash
      FROM users
      WHERE username = $username
    """.query[User].option

  def findById(id: UUID): ConnectionIO[Option[User]] =
    sql"""
      SELECT id, username, password_hash
      FROM users
      WHERE id = $id
    """.query[User].option

  def findAll: ConnectionIO[List[User]] =
    sql"""
      SELECT id, username, password_hash
      FROM users
    """.query[User].to[List]