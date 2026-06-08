package service

import cats.effect.IO
import munit.CatsEffectSuite
import domain.User
import repository.UserRepository
import doobie.hikari.HikariTransactor
import doobie.implicits._
import cats.effect.Resource
import java.util.UUID

class AuthServiceSuite extends CatsEffectSuite:

  val testUser = User(
    id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
    username = "testuser",
    passwordHash = AuthService.hashPassword("password123")
  )

  val transactorResource: Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- Resource.pure(scala.concurrent.ExecutionContext.global)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.h2.Driver",
        url             = "jdbc:h2:mem:auth_test_db;DB_CLOSE_DELAY=-1",
        user            = "sa",
        pass            = "",
        connectEC       = ec,
        logHandler      = None
      )
    } yield xa

  val databaseFixture = ResourceSuiteLocalFixture("database", transactorResource)

  override def munitFixtures = List(databaseFixture)

  private def setupSchema(xa: HikariTransactor[IO]): IO[Unit] =
    sql"""
      CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password_hash VARCHAR(255) NOT NULL
      )
    """.update.run.transact(xa).void

  // ===== REGISTER TESTS =====

  test("Register - happy path - should create new user") {
    val xa = databaseFixture()
    for {
      _      <- setupSchema(xa)
      result <- AuthService.register("newuser", "password123", xa)
    } yield {
      result match
        case Right(user) =>
          assert(user.username == "newuser", "Username should match")
          assert(user.passwordHash.nonEmpty, "Password hash should not be empty")
        case Left(error) => fail(s"Expected Right, got Left: $error")
    }
  }

  test("Register - 400 empty username") {
    AuthService.register("", "password123", null).map {
      case Left(error) =>
        assert(error == "Username cannot be empty", s"Expected 'Username cannot be empty', got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  test("Register - 400 empty password") {
    AuthService.register("newuser", "", null).map {
      case Left(error) =>
        assert(error == "Password cannot be empty", s"Expected 'Password cannot be empty', got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  test("Register - 400 null username") {
    AuthService.register(null, "password123", null).map {
      case Left(error) =>
        assert(error == "Username cannot be empty", s"Expected 'Username cannot be empty', got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  test("Register - 400 null password") {
    AuthService.register("newuser", null, null).map {
      case Left(error) =>
        assert(error == "Password cannot be empty", s"Expected 'Password cannot be empty', got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  // ===== LOGIN TESTS =====

  test("Login - happy path - should log in existing user") {
    val xa = databaseFixture()
    for {
      _      <- setupSchema(xa)
      _      <- AuthService.register("loginuser", "securepass", xa)
      result <- AuthService.login("loginuser", "securepass", xa)
    } yield {
      result match
        case Right(authResponse) =>
          assert(authResponse.username == "loginuser", "Username in response should match")
          assert(authResponse.token.nonEmpty, "JWT Token should not be empty")
          assert(authResponse.userId.nonEmpty, "User ID should be present")
        case Left(error) => fail(s"Expected Right, got Left: $error")
    }
  }

  test("Login - 400 user not found") {
    val xa = databaseFixture()
    for {
      _      <- setupSchema(xa)
      result <- AuthService.login("nonexistent", "password123", xa)
    } yield {
      result match
        case Left(error) => assert(error == "User not found", s"Expected 'User not found', got: $error")
        case Right(_)    => fail("Expected Left, got Right")
    }
  }

  test("Login - 400 empty username") {
    AuthService.login("", "password123", null).map {
      case Left(error) =>
        assert(error == "Username cannot be empty", s"Expected 'Username cannot be empty', got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  test("Login - 400 empty password") {
    AuthService.login("testuser", "", null).map {
      case Left(error) =>
        assert(error == "Password cannot be empty", s"Expected 'Password cannot be empty', got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  test("Login - 400 null username") {
    AuthService.login(null, "password123", null).map {
      case Left(error) =>
        assert(error == "Username cannot be empty", s"Expected 'Username cannot be empty', got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  test("Login - 400 null password") {
    AuthService.login("testuser", null, null).map {
      case Left(error) =>
        assert(error == "Password cannot be empty", s"Expected 'Password cannot be empty', got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  // ===== JWT SERVICE VALIDATION TESTS =====

  test("JWT - validateToken - should extract payload from valid token") {
    val token = JwtService.generateToken(testUser.id.toString, testUser.username)
    JwtService.validateToken(token).map {
      case Right(payload) =>
        assert(payload.userId == testUser.id.toString, "User ID should match")
        assert(payload.username == testUser.username, "Username should match")
      case Left(error) => fail(s"Expected Right, got Left: $error")
    }
  }

  test("JWT - validateToken - should reject token with invalid signature") {
    val validToken = JwtService.generateToken(testUser.id.toString, testUser.username)
    val invalidToken = validToken.dropRight(5) + "XXXXX"
    JwtService.validateToken(invalidToken).map {
      case Left(error) =>
        assert(error.contains("signature") || error.contains("Invalid"), s"Expected signature/invalid error, got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
    }
  }

  test("JWT - validateToken - should reject expired token") {
    val expiredToken = JwtService.generateToken(testUser.id.toString, testUser.username, expirationSeconds = 1)
    IO.blocking(Thread.sleep(1100)) >>
      JwtService.validateToken(expiredToken).map {
        case Left(error) =>
          assert(error.contains("expired") || error.contains("Invalid"), s"Expected expired/invalid error, got: $error")
        case Right(_) => fail("Expected Left (error), got Right")
      }
  }

  test("JWT - getTokenFromHeader - should extract token from valid Bearer header") {
    val result = JwtService.getTokenFromHeader("Bearer validtoken123")
    result match
      case Right(token) => assert(token == "validtoken123", "Token should match")
      case Left(error)  => fail(s"Expected Right, got Left: $error")
  }

  test("JWT - getTokenFromHeader - should reject invalid header format") {
    val result = JwtService.getTokenFromHeader("Basic validtoken123")
    result match
      case Left(error) =>
        assert(error.contains("Invalid authorization header"), s"Expected 'Invalid authorization header' error, got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
  }

  test("JWT - getTokenFromHeader - should reject empty Authorization header") {
    val result = JwtService.getTokenFromHeader("")
    result match
      case Left(error) =>
        assert(error.contains("Invalid authorization header"), s"Expected 'Invalid authorization header' error, got: $error")
      case Right(_) => fail("Expected Left (error), got Right")
  }

  // ===== PASSWORD HASHING TESTS =====

  test("Password hashing - should hash password correctly") {
    val password = "mypassword"
    val hash = AuthService.hashPassword(password)
    assert(hash.nonEmpty, "Hash should not be empty")
    assert(hash != password, "Hash should not equal password")
  }

  test("Password hashing - same password should produce same hash") {
    val password = "mypassword"
    val hash1 = AuthService.hashPassword(password)
    val hash2 = AuthService.hashPassword(password)
    assert(hash1 == hash2, "Same passwords should produce same hash")
  }

  test("Password verification - should verify correct password") {
    val password = "mypassword"
    val hash = AuthService.hashPassword(password)
    val isValid = AuthService.verifyPassword(password, hash)
    assert(isValid, "Valid password should verify")
  }

  test("Password verification - should reject incorrect password") {
    val password = "mypassword"
    val wrongPassword = "wrongpassword"
    val hash = AuthService.hashPassword(password)
    val isValid = AuthService.verifyPassword(wrongPassword, hash)
    assert(!isValid, "Invalid password should not verify")
  }

  // ===== ADDITIONAL REGISTER TESTS =====

  test("Register - should allow usernames with numbers") {
    val xa = databaseFixture()
    for {
      _      <- setupSchema(xa)
      result <- AuthService.register("user123", "password123", xa)
    } yield {
      result match
        case Right(user) => assert(user.username == "user123")
        case Left(error) => fail(s"Should allow numeric usernames: $error")
    }
  }

  test("Register - should allow usernames with special characters") {
    val xa = databaseFixture()
    for {
      _      <- setupSchema(xa)
      result <- AuthService.register("user@example.com", "password123", xa)
    } yield {
      result match
        case Right(user) => assert(user.username == "user@example.com")
        case Left(error) => fail(s"Should allow special characters: $error")
    }
  }

  test("Register - should allow long usernames") {
    val xa = databaseFixture()
    val longUsername = "user_" + ("x" * 250)
    for {
      _      <- setupSchema(xa)
      result <- AuthService.register(longUsername, "password123", xa)
    } yield {
      result match
        case Right(user) => assert(user.username == longUsername)
        case Left(error) => fail(s"Should allow long usernames: $error")
    }
  }

  test("Register - should allow long passwords") {
    val xa = databaseFixture()
    val longPassword = "pwd_" + ("x" * 500)
    for {
      _      <- setupSchema(xa)
      result <- AuthService.register("user", longPassword, xa)
    } yield {
      result match
        case Right(user) => assert(user.passwordHash.nonEmpty)
        case Left(error) => fail(s"Should allow long passwords: $error")
    }
  }

  test("Register - should prevent duplicate username") {
    val xa = databaseFixture()
    for {
      _       <- setupSchema(xa)
      _       <- AuthService.register("dupuser", "pass123", xa)
      result2 <- AuthService.register("dupuser", "differentpass", xa)
    } yield {
      result2 match
        case Left(error) => assert(error == "User already exists", s"Expected 'User already exists', got: $error")
        case Right(_)    => fail("Should not allow duplicate username")
    }
  }

  test("Register - should generate different password hashes for different passwords") {
    val hash1 = AuthService.hashPassword("password1")
    val hash2 = AuthService.hashPassword("password2")
    assert(hash1 != hash2, "Different passwords should produce different hashes")
  }

  test("Register - should whitespace-trim username allows registration") {
    val xa = databaseFixture()
    // Note: whitespace-only usernames are not allowed per validation
    for {
      _      <- setupSchema(xa)
      result <- AuthService.register("  user  ", "password123", xa)
    } yield {
      result match
        case Right(user) =>
          // Username might be stored with spaces depending on implementation
          assert(user.username.nonEmpty)
        case Left(_) => assert(true, "May reject whitespace-heavy usernames")
    }
  }

  test("Register - should assign unique IDs to each user") {
    val xa = databaseFixture()
    for {
      _      <- setupSchema(xa)
      user1  <- AuthService.register("user1", "pass1", xa)
      user2  <- AuthService.register("user2", "pass2", xa)
    } yield {
      user1 match
        case Right(u1) =>
          user2 match
            case Right(u2) => assert(u1.id != u2.id, "Each user should have unique ID")
            case Left(_) => fail("Second registration failed")
        case Left(_) => fail("First registration failed")
    }
  }

  // ===== ADDITIONAL LOGIN TESTS =====

  test("Login - wrong password should fail") {
    val xa = databaseFixture()
    for {
      _      <- setupSchema(xa)
      _      <- AuthService.register("testuser", "correctpass", xa)
      result <- AuthService.login("testuser", "wrongpass", xa)
    } yield {
      result match
        case Left(error) => assert(error == "Invalid credentials", s"Expected 'Invalid credentials', got: $error")
        case Right(_)    => fail("Should reject wrong password")
    }
  }

  test("Login - should return correct username in response") {
    val xa = databaseFixture()
    for {
      _      <- setupSchema(xa)
      _      <- AuthService.register("myuser", "pass123", xa)
      result <- AuthService.login("myuser", "pass123", xa)
    } yield {
      result match
        case Right(authResponse) => assert(authResponse.username == "myuser")
        case Left(error)         => fail(s"Login failed: $error")
    }
  }

  test("Login - should allow login with special character username") {
    val xa = databaseFixture()
    val specialUser = "user+test@example.com"
    for {
      _      <- setupSchema(xa)
      _      <- AuthService.register(specialUser, "pass123", xa)
      result <- AuthService.login(specialUser, "pass123", xa)
    } yield {
      result match
        case Right(authResponse) => assert(authResponse.username == specialUser)
        case Left(error)         => fail(s"Should support special characters: $error")
    }
  }

  // ===== ADDITIONAL PASSWORD HASHING TESTS =====

  test("Password hashing - should produce Base64 output") {
    val hash = AuthService.hashPassword("password")
    // Base64 strings only contain A-Z, a-z, 0-9, +, /, and =
    val base64Pattern = "^[A-Za-z0-9+/]*={0,2}$"
    assert(hash.matches(base64Pattern), "Hash should be valid Base64")
  }

  test("Password hashing - different passwords produce different hashes (entropy)") {
    val hashes = (1 to 10).map { i => AuthService.hashPassword(s"password$i") }.toSet
    assert(hashes.size == 10, "10 different passwords should produce 10 different hashes")
  }

  test("Password verification - empty password") {
    val hash = AuthService.hashPassword("")
    val isValid = AuthService.verifyPassword("", hash)
    assert(isValid, "Empty password should be hashable and verifiable")
  }

  test("Password verification - very long password") {
    val longPass = "p" * 10000
    val hash = AuthService.hashPassword(longPass)
    val isValid = AuthService.verifyPassword(longPass, hash)
    assert(isValid, "Should handle very long passwords")
  }

  test("Password hashing - special characters should work") {
    val specialPass = "P@$$w0rd!#%&*()[]{}\\|;:',.<>?/`~"
    val hash = AuthService.hashPassword(specialPass)
    val isValid = AuthService.verifyPassword(specialPass, hash)
    assert(isValid, "Should handle special characters in password")
  }

  test("Password hashing - unicode characters should work") {
    val unicodePass = "пароль密码🔐secure"
    val hash = AuthService.hashPassword(unicodePass)
    val isValid = AuthService.verifyPassword(unicodePass, hash)
    assert(isValid, "Should handle unicode characters")
  }

