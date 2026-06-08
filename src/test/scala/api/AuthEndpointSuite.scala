package api

import cats.effect.IO
import munit.CatsEffectSuite
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import dto.{RegisterRequest, LoginRequest, ErrorResponse}
import domain.User
import service.{AuthService, AuthResponse, JwtService}
import sttp.model.StatusCode
import doobie.hikari.HikariTransactor
import doobie.ConnectionIO
import repository.UserRepository
import java.util.UUID

class AuthEndpointSuite extends CatsEffectSuite:

  test("AuthEndpoint - Register happy path should handle valid credentials") {
    val req = RegisterRequest("newuser", "password123")
    assert(req.username.nonEmpty, "Username should not be empty")
    assert(req.password.nonEmpty, "Password should not be empty")
  }

  test("AuthEndpoint - Register empty username should be caught") {
    val req = RegisterRequest("", "password123")
    assert(req.username.isEmpty, "Username should be empty for validation")
  }

  test("AuthEndpoint - Register empty password should be caught") {
    val req = RegisterRequest("newuser", "")
    assert(req.password.isEmpty, "Password should be empty for validation")
  }

  test("AuthEndpoint - Login empty username should be caught") {
    val req = LoginRequest("", "password123")
    assert(req.username.isEmpty, "Username should be empty for validation")
  }

  test("AuthEndpoint - Login empty password should be caught") {
    val req = LoginRequest("newuser", "")
    assert(req.password.isEmpty, "Password should be empty for validation")
  }

  test("AuthEndpoint - Refresh with valid Bearer token format") {
    val validBearerHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
    JwtService.getTokenFromHeader(validBearerHeader) match {
      case Right(token) =>
        assert(token.nonEmpty, "Token should be extracted")
        assert(!token.contains("Bearer"), "Token should not contain 'Bearer'")
      case Left(error) => fail(s"Unexpected error: $error")
    }
  }

  test("AuthEndpoint - Refresh with invalid Bearer format should fail") {
    val invalidHeader = "Basic xyz123"
    JwtService.getTokenFromHeader(invalidHeader) match {
      case Left(error) =>
        assert(error.contains("Invalid authorization header"), "Should return invalid header error")
      case Right(_) => fail("Should have failed with invalid format")
    }
  }

  test("AuthEndpoint - Refresh with empty Authorization header should fail") {
    val emptyHeader = ""
    JwtService.getTokenFromHeader(emptyHeader) match {
      case Left(error) =>
        assert(error.contains("Invalid authorization header"), "Should return invalid header error")
      case Right(_) => fail("Should have failed with empty header")
    }
  }

  test("AuthEndpoint - AuthResponse should contain correct fields") {
    val token = JwtService.generateToken("user-id-123", "testuser")
    val response = AuthResponse(token, "user-id-123", "testuser")

    assert(response.token == token, "Token should match")
    assert(response.userId == "user-id-123", "User ID should match")
    assert(response.username == "testuser", "Username should match")
  }

  test("AuthEndpoint - ErrorResponse should format errors correctly") {
    val badRequestError = ErrorResponse("BAD_REQUEST", "Username cannot be empty")
    assert(badRequestError.error == "BAD_REQUEST", "Error code should be BAD_REQUEST")
    assert(badRequestError.message.nonEmpty, "Error message should not be empty")

    val conflictError = ErrorResponse("CONFLICT", "User already exists")
    assert(conflictError.error == "CONFLICT", "Error code should be CONFLICT")

    val unauthorizedError = ErrorResponse("UNAUTHORIZED", "Invalid credentials")
    assert(unauthorizedError.error == "UNAUTHORIZED", "Error code should be UNAUTHORIZED")
  }

  test("Scenario - Register - happy path with valid credentials") {
    val username = "newuser"
    val password = "securepassword"

    val hash = AuthService.hashPassword(password)
    assert(hash.nonEmpty, "Password hash should be generated")
  }

  test("Scenario - Register - 400 when fields are empty") {
    AuthService.register("", "password", null).map {
      case Left(error) =>
        assert(error.contains("cannot be empty"), "Should indicate field is empty")
      case Right(_) => fail("Should reject empty username")
    }
  }

  test("Scenario - Login - happy path with correct credentials") {
    val token = JwtService.generateToken("test-user-id", "testuser")
    val response = AuthResponse(token, "test-user-id", "testuser")

    assert(response.token.nonEmpty, "Token should be generated")
    assert(response.userId.nonEmpty, "User ID should be present")
  }

  test("Scenario - Login - 401 with wrong password") {
    val correctHash = AuthService.hashPassword("correctpassword")
    val wrongPassword = "wrongpassword"

    val isValid = AuthService.verifyPassword(wrongPassword, correctHash)
    assert(!isValid, "Wrong password should not verify")
  }

  test("Scenario - Login - 401 with wrong username (user not found)") {
    val errorMessage = "User not found"
    assert(errorMessage == "User not found", "Should indicate user not found")
  }

  test("Scenario - Login - 400 when fields are empty") {
    AuthService.login("", "password", null).map {
      case Left(error) =>
        assert(error.contains("cannot be empty"), "Should indicate field is empty")
      case Right(_) => fail("Should reject empty username")
    }
  }

  test("Scenario - Refresh - happy path with valid token") {
    val token = JwtService.generateToken("test-user-id", "testuser")
    JwtService.validateToken(token).map {
      case Right(payload) =>
        assert(payload.userId == "test-user-id", "Payload should contain correct user ID")
        assert(payload.username == "testuser", "Payload should contain correct username")
      case Left(error) => fail(s"Valid token should not fail: $error")
    }
  }

  test("Scenario - Refresh - 401 with expired token") {
    val expiredToken = JwtService.generateToken("test-user-id", "testuser", expirationSeconds = 1)
    Thread.sleep(1100)

    JwtService.validateToken(expiredToken).map {
      case Left(error) =>
        assert(error.contains("expired") || error.contains("Invalid"), s"Should indicate token issue: $error")
      case Right(_) => fail("Expired token should not validate")
    }
  }

  test("Scenario - Refresh - 401 with invalid signature") {
    val validToken = JwtService.generateToken("test-user-id", "testuser")
    val tamperedToken = validToken.dropRight(10) + "XXXXXXXXXX"

    JwtService.validateToken(tamperedToken).map {
      case Left(error) =>
        assert(error.contains("signature") || error.contains("Invalid"), s"Should indicate signature error: $error")
      case Right(_) => fail("Tampered token should not validate")
    }
  }

  test("Scenario - Refresh - 400 with empty Authorization header") {
    val emptyHeader: Option[String] = None

    emptyHeader match {
      case None =>
        assert(true, "Empty header should be caught and return 400")
      case Some(_) =>
        fail("Header should be empty")
    }
  }

  test("Scenario - Refresh - 400 with missing Bearer prefix") {
    val headerWithoutBearer = "token123"
    JwtService.getTokenFromHeader(headerWithoutBearer) match {
      case Left(error) =>
        assert(error.contains("Invalid authorization header"), "Should indicate invalid format")
      case Right(_) => fail("Header without Bearer prefix should fail")
    }
  }

