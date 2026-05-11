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