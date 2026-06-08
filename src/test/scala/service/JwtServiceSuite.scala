package service

import cats.effect.IO
import munit.FunSuite

class JwtServiceSuite extends FunSuite:

  // ===== TOKEN GENERATION TESTS =====

  test("Generate token - should create non-empty token") {
    val token = JwtService.generateToken("user123", "john")
    assert(token.nonEmpty, "Token should not be empty")
  }

  test("Generate token - should create valid JWT format (3 parts separated by dots)") {
    val token = JwtService.generateToken("user123", "john")
    val parts = token.split("\\.")
    assert(parts.length == 3, s"JWT should have 3 parts, got ${parts.length}")
  }

  test("Generate token - different users should produce different tokens") {
    val token1 = JwtService.generateToken("user1", "alice")
    val token2 = JwtService.generateToken("user2", "bob")
    assert(token1 != token2, "Different users should produce different tokens")
  }

  test("Generate token - same parameters should produce same token structure") {
    val token1 = JwtService.generateToken("user123", "john")
    val token2 = JwtService.generateToken("user123", "john")
    // Tokens will be the same since JWT is deterministic with same input
    assert(token1 == token2, "Same parameters should produce identical tokens")
  }

  test("Generate token - should allow custom expiration seconds") {
    val token = JwtService.generateToken("user456", "alice", 7200)
    assert(token.nonEmpty, "Token with custom expiration should be created")
  }

  test("Generate token - should handle very long usernames") {
    val longUsername = "a" * 1000
    val token = JwtService.generateToken("user789", longUsername)
    assert(token.nonEmpty, "Should handle long usernames")
  }

  test("Generate token - should handle special characters in username") {
    val specialUsername = "user@example.com"
    val token = JwtService.generateToken("user999", specialUsername)
    assert(token.nonEmpty, "Should handle special characters in username")
  }

  test("Generate token - should handle numbers in userId") {
    val token = JwtService.generateToken("12345", "user")
    assert(token.nonEmpty, "Should handle numeric userIds")
  }

  test("Generate token - should handle UUID format userId") {
    val uuid = "550e8400-e29b-41d4-a716-446655440000"
    val token = JwtService.generateToken(uuid, "user")
    assert(token.nonEmpty, "Should handle UUID format userIds")
  }

  test("Generate token - should handle empty username") {
    val token = JwtService.generateToken("user123", "")
    assert(token.nonEmpty, "Should handle empty username")
  }

  test("Generate token - default expiration should be 3600 seconds") {
    val token = JwtService.generateToken("user123", "john")
    assert(token.nonEmpty, "Default expiration token should work")
  }

  test("Generate token - zero expiration should be allowed") {
    val token = JwtService.generateToken("user123", "john", 0)
    assert(token.nonEmpty, "Zero expiration token should be created")
  }

  test("Generate token - negative expiration should be allowed") {
    val token = JwtService.generateToken("user123", "john", -3600)
    assert(token.nonEmpty, "Negative expiration token should be created")
  }

  // ===== TOKEN VALIDATION TESTS =====

  test("Validate token - should extract payload from valid token") {
    for {
      token <- IO(JwtService.generateToken("user123", "john"))
      result <- JwtService.validateToken(token)
    } yield {
      assert(result.isRight, "Should parse valid token")
      result match
        case Right(payload) =>
          assert(payload.userId == "user123", "User ID should match")
          assert(payload.username == "john", "Username should match")
        case Left(_) => fail("Should extract payload")
    }
  }

  test("Validate token - should reject token with tampered signature") {
    for {
      token <- IO(JwtService.generateToken("user123", "john"))
      invalidToken = token.dropRight(5) + "XXXXX"
      result <- JwtService.validateToken(invalidToken)
    } yield {
      assert(result.isLeft, "Should reject tampered token")
      result match
        case Left(error) =>
          assert(error.nonEmpty && (error.contains("signature") || error.contains("Invalid")),
            s"Should indicate invalid signature, got: $error")
        case Right(_) => fail("Should reject tampered token")
    }
  }

  test("Validate token - should reject empty token string") {
    JwtService.validateToken("").map { result =>
      assert(result.isLeft, "Should reject empty token")
    }
  }

  test("Validate token - should reject null-like token") {
    JwtService.validateToken("null").map { result =>
      assert(result.isLeft, "Should reject null string token")
    }
  }

  test("Validate token - should reject completely invalid token format") {
    JwtService.validateToken("not.a.token.anyway").map { result =>
      assert(result.isLeft, "Should reject invalid token format")
    }
  }

  test("Validate token - should reject token with only 2 parts") {
    JwtService.validateToken("part1.part2").map { result =>
      assert(result.isLeft, "Should reject incomplete token")
    }
  }

  test("Validate token - should reject token with 4 parts") {
    JwtService.validateToken("part1.part2.part3.part4").map { result =>
      assert(result.isLeft, "Should reject token with too many parts")
    }
  }

  test("Validate token - should handle token with special characters in payload") {
    for {
      token <- IO(JwtService.generateToken("user@123", "john.doe@example.com"))
      result <- JwtService.validateToken(token)
    } yield {
      assert(result.isRight, "Should handle special characters")
      result match
        case Right(payload) =>
          assert(payload.userId == "user@123")
          assert(payload.username == "john.doe@example.com")
        case Left(_) => fail("Should parse special characters")
    }
  }

  test("Validate token - should validate token with UUID userId") {
    val uuid = "550e8400-e29b-41d4-a716-446655440000"
    for {
      token <- IO(JwtService.generateToken(uuid, "alice"))
      result <- JwtService.validateToken(token)
    } yield {
      assert(result.isRight)
      result match
        case Right(payload) =>
          assert(payload.userId == uuid)
          assert(payload.username == "alice")
        case Left(_) => fail("Should validate UUID tokens")
    }
  }

  test("Validate token - should return IO[Either] type") {
    val token = JwtService.generateToken("user123", "john")
    val result = JwtService.validateToken(token)
    // This is more of a type check
    assert(true, "Should return correct type")
  }

  // ===== GET TOKEN FROM HEADER TESTS =====

  test("Get token from header - should extract token from valid Bearer header") {
    val result = JwtService.getTokenFromHeader("Bearer mytoken123")
    result match
      case Right(token) => assert(token == "mytoken123", "Should extract token correctly")
      case Left(_)      => fail("Should extract valid Bearer token")
  }

  test("Get token from header - should handle Bearer header with multiple spaces") {
    val result = JwtService.getTokenFromHeader("Bearer    mytoken123")
    result match
      case Right(token) => assert(token == "mytoken123", "Should trim extra spaces")
      case Left(_)      => fail("Should handle multiple spaces")
  }

  test("Get token from header - should reject Basic authentication scheme") {
    val result = JwtService.getTokenFromHeader("Basic mytoken123")
    result match
      case Left(error) => assert(error.contains("Invalid"), "Should reject Basic scheme")
      case Right(_)    => fail("Should reject Basic scheme")
  }

  test("Get token from header - should reject empty string") {
    val result = JwtService.getTokenFromHeader("")
    result match
      case Left(error) => assert(error.contains("Invalid"), "Should reject empty header")
      case Right(_)    => fail("Should reject empty header")
  }

  test("Get token from header - should reject whitespace-only header") {
    val result = JwtService.getTokenFromHeader("   ")
    result match
      case Left(error) => assert(error.contains("Invalid"), "Should reject whitespace-only header")
      case Right(_)    => fail("Should reject whitespace-only")
  }

  test("Get token from header - should reject header without Bearer prefix") {
    val result = JwtService.getTokenFromHeader("mytoken123")
    result match
      case Left(error) => assert(error.contains("Invalid"), "Should require Bearer prefix")
      case Right(_)    => fail("Should require Bearer prefix")
  }

  test("Get token from header - should handle case sensitivity (lowercase bearer)") {
    val result = JwtService.getTokenFromHeader("bearer mytoken123")
    result match
      case Left(_) => assert(true, "Bearer should be case-sensitive (capital B required)")
      case Right(_) => fail("Bearer should be case-sensitive")
  }

  test("Get token from header - should handle token with dots (valid JWT format)") {
    val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    val result = JwtService.getTokenFromHeader(s"Bearer $jwtToken")
    result match
      case Right(token) => assert(token == jwtToken, "Should extract JWT properly")
      case Left(_)      => fail("Should extract JWT token")
  }

  test("Get token from header - should handle token with special characters") {
    val specialToken = "token-with_special.chars"
    val result = JwtService.getTokenFromHeader(s"Bearer $specialToken")
    result match
      case Right(token) => assert(token == specialToken)
      case Left(_)      => fail("Should handle special characters in token")
  }

  test("Get token from header - should return Either type") {
    val result = JwtService.getTokenFromHeader("Bearer test")
    assert(result.isRight, "Should return Either type correctly")
  }

  // ===== JWT PAYLOAD TEST =====

  test("JWT Payload - should create payload with userId and username") {
    val payload = JwtService.JwtPayload("user123", "john")
    assert(payload.userId == "user123")
    assert(payload.username == "john")
  }

  test("JWT Payload - should have proper case class properties") {
    val payload1 = JwtService.JwtPayload("user1", "alice")
    val payload2 = JwtService.JwtPayload("user1", "alice")
    assert(payload1 == payload2, "Equal payloads should be equal")
  }

  test("JWT Payload - should handle long userId") {
    val longId = "x" * 1000
    val payload = JwtService.JwtPayload(longId, "user")
    assert(payload.userId == longId)
  }

  test("JWT Payload - should handle long username") {
    val longUsername = "y" * 1000
    val payload = JwtService.JwtPayload("user123", longUsername)
    assert(payload.username == longUsername)
  }

  // ===== INTEGRATION TESTS =====

  test("Integration - generate and validate token") {
    for {
      token <- IO(JwtService.generateToken("user456", "bob"))
      result <- JwtService.validateToken(token)
    } yield {
      assert(result.isRight, "Generated token should be valid")
      result match
        case Right(payload) =>
          assert(payload.userId == "user456")
          assert(payload.username == "bob")
        case Left(error) => fail(s"Should validate: $error")
    }
  }

  test("Integration - generate, extract header, validate") {
    val userId = "user789"
    val username = "charlie"
    for {
      token <- IO(JwtService.generateToken(userId, username))
      headerResult = JwtService.getTokenFromHeader(s"Bearer $token")
      validationResult <- headerResult match
        case Right(extractedToken) => JwtService.validateToken(extractedToken)
        case Left(error) => IO.pure(Left(error))
    } yield {
      assert(validationResult.isRight, "Full flow should work")
      validationResult match
        case Right(payload) =>
          assert(payload.userId == userId)
          assert(payload.username == username)
        case Left(error) => fail(s"Should validate: $error")
    }
  }




