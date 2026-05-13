scalaVersion := "3.3.1"

name := "travel-planner"
version := "0.1.0-SNAPSHOT"

val tapirVersion = "1.9.6"
val http4sVersion = "0.23.25"
val doobieVersion = "1.0.0-RC4"
val circeVersion = "0.14.6"

libraryDependencies ++= Seq(
  // Tapir
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion,

  // HTTP Server
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion,

  // Doobie
  "org.tpolecat" %% "doobie-core"     % doobieVersion,
  "org.tpolecat" %% "doobie-hikari"   % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,

  // JSON (Circe)
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion,

  "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
  "org.slf4j"      % "slf4j-simple"   % "2.0.9",

  "com.github.jwt-scala" %% "jwt-core" % "9.4.0",
  "com.github.jwt-scala" %% "jwt-circe" % "9.4.0"
)

scalacOptions ++= Seq(
  "-rewrite",
  "-source", "3.3"
)