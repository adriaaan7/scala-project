package db

import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database:
  def makeTransactor: Resource[IO, HikariTransactor[IO]] =
    for
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        "jdbc:postgresql://localhost:5432/postgres",
        "postgres",
        "password",
        ce
      )
    yield xa
