scalaVersion := "3.3.1"
name := "travel-planner"
version := "0.1.0-SNAPSHOT"

val tapirVersion = "1.9.6"
val http4sVersion = "0.23.25"

libraryDependencies ++= Seq(
  // Tapir
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,

  // Swagger
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
  
  // HTTP Server
  "org.http4s" %% "http4s-ember-server" % http4sVersion
)

val doobieVersion = "1.0.0-RC4"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core"     % doobieVersion,
  "org.tpolecat" %% "doobie-hikari"   % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion
)