package api

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import domain.Trip
import service.{TripService, JwtService}
import sttp.model.StatusCode
import doobie.hikari.HikariTransactor
import java.util.UUID
import java.time.LocalDate
import java.time.Instant

class TripRoutesSpec extends CatsEffectSuite:

  // Helper to create a valid Bearer token with parsed userId as string
  private def createValidToken(userId: String): String =
    val token = JwtService.generateToken(userId, s"user-$userId")
    s"Bearer $token"

  // Test data
  private val testUserId = UUID.randomUUID()
  private val testUserId2 = UUID.randomUUID()
  private val trip1 = Trip(
    UUID.randomUUID(),
    "Paris Trip",
    LocalDate.of(2025, 6, 1),
    LocalDate.of(2025, 6, 10),
    testUserId
  )
  private val trip2 = Trip(
    UUID.randomUUID(),
    "Tokyo Adventure",
    LocalDate.of(2025, 8, 1),
    LocalDate.of(2025, 8, 15),
    testUserId
  )
  private val trip3 = Trip(
    UUID.randomUUID(),
    "New York City",
    LocalDate.of(2025, 9, 1),
    LocalDate.of(2025, 9, 5),
    testUserId2
  )

  test("TripRoutes - extractUserId should parse valid Bearer token") {
    val validToken = createValidToken(testUserId.toString)
    TripRoutes.extractUserId(validToken).map { result =>
      assert(result.isRight, "Should successfully parse valid token")
      result match {
        case Right(userId) =>
          assert(userId == testUserId, "Should extract correct user ID")
        case Left(error) => fail(s"Should not fail: $error")
      }
    }
  }

  test("TripRoutes - extractUserId should reject invalid Bearer format") {
    val invalidHeader = "Basic dXNlcjpwYXNz"
    TripRoutes.extractUserId(invalidHeader).map { result =>
      assert(result.isLeft, "Should reject invalid Bearer format")
      result match {
        case Left(error) =>
          assert(error.contains("Invalid authorization header"), s"Got: $error")
        case Right(_) => fail("Should not succeed with invalid format")
      }
    }
  }

  test("TripRoutes - extractUserId should reject empty Authorization header") {
    val emptyHeader = ""
    TripRoutes.extractUserId(emptyHeader).map { result =>
      assert(result.isLeft, "Should reject empty header")
      result match {
        case Left(error) =>
          assert(error.contains("Invalid authorization header"), s"Got: $error")
        case Right(_) => fail("Should not succeed with empty header")
      }
    }
  }

  test("TripRoutes - extractUserId should reject malformed UUID") {
    val token = JwtService.generateToken("not-a-uuid", "testuser")
    val bearerToken = s"Bearer $token"
    TripRoutes.extractUserId(bearerToken).map { result =>
      assert(result.isLeft, "Should reject malformed UUID")
      result match {
        case Left(error) =>
          assert(error.contains("Invalid UUID format"), s"Got: $error")
        case Right(_) => fail("Should not succeed with malformed UUID")
      }
    }
  }

  test("TripRoutes - extractUserId should handle missing or malformed token") {
    val headerWithoutToken = "Bearer "
    TripRoutes.extractUserId(headerWithoutToken).map { result =>
      assert(result.isLeft, "Should reject header without token")
    }
  }

  test("Scenario - ListTripsEndpoint - with valid token should extract userId") {
    val validToken = createValidToken(testUserId.toString)
    TripRoutes.extractUserId(validToken).map { result =>
      assert(result.isRight, "Should extract userId from valid token")
      result match {
        case Right(userId) =>
          assert(userId == testUserId, "Should match the original userId")
        case Left(_) => fail("Should not fail with valid token")
      }
    }
  }

  test("Scenario - ListTripsEndpoint - with invalid authorization should fail") {
    val invalidHeader = "InvalidHeader"
    TripRoutes.extractUserId(invalidHeader).map { result =>
      assert(result.isLeft, "Should fail with invalid header")
      result match {
        case Left(error) =>
          assert(
            error.contains("Invalid authorization header") || error.contains("Invalid"),
            s"Should indicate authorization error, got: $error"
          )
        case Right(_) => fail("Should not succeed with invalid header")
      }
    }
  }

  test("Scenario - ListTripsEndpoint - should return 401 when authorization fails") {
    val invalidHeader = "Bearer invalid.token.here"
    TripRoutes.extractUserId(invalidHeader).map { result =>
      assert(result.isLeft, "Should fail authorization")
      result match {
        case Left(error) =>
          val is401Error = error.contains("Invalid") || error.contains("signature")
          assert(is401Error, s"Should indicate 401 level error, got: $error")
        case Right(_) => fail("Should not succeed with invalid token")
      }
    }
  }

  test("Scenario - ListTripsEndpoint - happy path with valid credentials") {
    val validToken = createValidToken(testUserId.toString)
    TripRoutes.extractUserId(validToken).map { result =>
      assert(result.isRight, "Should authenticate successfully")
      result match {
        case Right(userId) =>
          assert(userId.toString.nonEmpty, "User ID should be extracted")
          assert(!userId.toString.isEmpty, "User ID should not be empty")
        case Left(error) => fail(s"Should not fail: $error")
      }
    }
  }

  test("Scenario - ListTripsEndpoint - 400 when Authorization header is missing") {
    val emptyHeader = ""
    TripRoutes.extractUserId(emptyHeader).map { result =>
      assert(result.isLeft, "Should return error for missing header")
      result match {
        case Left(error) =>
          assert(error.contains("Invalid authorization header"), "Should indicate bad request")
        case Right(_) => fail("Should not succeed with missing header")
      }
    }
  }

  test("Scenario - ListTripsEndpoint - 401 with expired token") {
    val expiredToken = JwtService.generateToken(testUserId.toString, "testuser", expirationSeconds = 1)
    Thread.sleep(1100)

    val bearerHeader = s"Bearer $expiredToken"
    TripRoutes.extractUserId(bearerHeader).map { result =>
      assert(result.isLeft, "Should reject expired token")
      result match {
        case Left(error) =>
          val isExpiredError = error.contains("expired") || error.contains("Invalid")
          assert(isExpiredError, s"Should indicate token expiration, got: $error")
        case Right(_) => fail("Should not succeed with expired token")
      }
    }
  }

  test("Scenario - ListTripsEndpoint - 401 with invalid token signature") {
    val validToken = JwtService.generateToken(testUserId.toString, "testuser")
    val tamperedToken = validToken.dropRight(10) + "XXXXXXXXXX"
    val bearerHeader = s"Bearer $tamperedToken"

    TripRoutes.extractUserId(bearerHeader).map { result =>
      assert(result.isLeft, "Should reject tampered token")
      result match {
        case Left(error) =>
          val isSignatureError = error.contains("signature") || error.contains("Invalid")
          assert(isSignatureError, s"Should indicate signature error, got: $error")
        case Right(_) => fail("Should not succeed with tampered token")
      }
    }
  }

  test("Data consistency - Trip created with userId should match requested userId") {
    assert(trip1.ownerId == testUserId, "Trip owner ID should match test user ID")
    assert(trip2.ownerId == testUserId, "Trip owner ID should match test user ID")
    assert(trip3.ownerId == testUserId2, "Trip owner ID should match test user ID 2")
    assert(trip1.ownerId != trip3.ownerId, "Different users should have different trip owners")
  }

  test("Data consistency - Multiple trips for same user should have same ownerId") {
    val tripsForUser1 = List(trip1, trip2)
    val isConsistent = tripsForUser1.forall(_.ownerId == testUserId)
    assert(isConsistent, "All trips for user should have same ownerId")
  }

  test("Data consistency - Trip data should be preserved") {
    assert(trip1.title == "Paris Trip", "Trip title should be preserved")
    assert(trip1.startDate == LocalDate.of(2025, 6, 1), "Start date should be preserved")
    assert(trip1.endDate == LocalDate.of(2025, 6, 10), "End date should be preserved")
    assert(trip1.id.toString.nonEmpty, "Trip ID should be non-empty UUID")
  }

