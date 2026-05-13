package db

import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

object Database:
  def makeTransactor: Resource[IO, HikariTransactor[IO]] =
    for
      ce <- ExecutionContexts.fixedThreadPool[IO](32)
      host = System.getenv().getOrDefault("DB_HOST", "localhost")
      port = System.getenv().getOrDefault("DB_PORT", "5432")
      user = System.getenv().getOrDefault("DB_USER", "postgres")
      password = System.getenv().getOrDefault("DB_PASSWORD", "password")
      database = System.getenv().getOrDefault("DB_NAME", "postgres")
      url = s"jdbc:postgresql://$host:$port/$database"
      _ <- Resource.eval(IO.println(s"Connecting to: $url"))
      xa <- HikariTransactor.newHikariTransactor[IO](
        "org.postgresql.Driver",
        url,
        user,
        password,
        ce
      )
    yield xa