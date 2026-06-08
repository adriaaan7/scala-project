package service

import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import doobie.implicits.*
import munit.CatsEffectSuite

import java.time.LocalDate
import java.util.UUID

class TripServiceSuite extends CatsEffectSuite:

  val transactorResource: Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- Resource.pure(scala.concurrent.ExecutionContext.global)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.h2.Driver",
        url             = "jdbc:h2:mem:trip_test_db;DB_CLOSE_DELAY=-1",
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
      CREATE TABLE IF NOT EXISTS trips (
        id UUID PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        start_date DATE NOT NULL,
        end_date DATE NOT NULL,
        owner_id UUID NOT NULL
      )
    """.update.run.transact(xa).void

  // ===== CREATE TRIP TESTS =====

  test("Create trip - should create new trip with all fields") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.of(2024, 6, 1)
    val endDate = LocalDate.of(2024, 6, 10)

    for {
      _ <- setupSchema(xa)
      result <- TripService.createTrip("Summer Vacation", startDate, endDate, ownerId, xa)
    } yield {
      assert(result.title == "Summer Vacation", "Title should match")
      assert(result.startDate == startDate, "Start date should match")
      assert(result.endDate == endDate, "End date should match")
      assert(result.ownerId == ownerId, "Owner ID should match")
      assert(result.id != null, "ID should be generated")
    }
  }

  test("Create trip - should generate unique UUID") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.of(2024, 7, 1)
    val endDate = LocalDate.of(2024, 7, 15)

    for {
      _ <- setupSchema(xa)
      trip1 <- TripService.createTrip("Trip 1", startDate, endDate, ownerId, xa)
      trip2 <- TripService.createTrip("Trip 2", startDate, endDate, ownerId, xa)
    } yield {
      assert(trip1.id != trip2.id, "Each trip should have unique ID")
    }
  }

  test("Create trip - should handle dates in same day") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val sameDate = LocalDate.of(2024, 6, 15)

    for {
      _ <- setupSchema(xa)
      result <- TripService.createTrip("Day Trip", sameDate, sameDate, ownerId, xa)
    } yield {
      assert(result.startDate == sameDate)
      assert(result.endDate == sameDate)
    }
  }

  test("Create trip - should handle end date before start date") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.of(2024, 6, 15)
    val endDate = LocalDate.of(2024, 6, 1)

    for {
      _ <- setupSchema(xa)
      result <- TripService.createTrip("Backward Trip", startDate, endDate, ownerId, xa)
    } yield {
      // Service allows this - it's a business logic decision
      assert(result.startDate == startDate)
      assert(result.endDate == endDate)
    }
  }

  test("Create trip - should handle future dates") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val futureStart = LocalDate.now().plusYears(1)
    val futureEnd = futureStart.plusDays(10)

    for {
      _ <- setupSchema(xa)
      result <- TripService.createTrip("Future Trip", futureStart, futureEnd, ownerId, xa)
    } yield {
      assert(result.startDate == futureStart)
      assert(result.endDate == futureEnd)
    }
  }

  test("Create trip - should handle past dates") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val pastStart = LocalDate.of(2020, 1, 1)
    val pastEnd = LocalDate.of(2020, 1, 15)

    for {
      _ <- setupSchema(xa)
      result <- TripService.createTrip("Past Trip", pastStart, pastEnd, ownerId, xa)
    } yield {
      assert(result.startDate == pastStart)
    }
  }

  test("Create trip - should handle multiple trips for same owner") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(5)

    for {
      _ <- setupSchema(xa)
      trip1 <- TripService.createTrip("Trip A", startDate, endDate, ownerId, xa)
      trip2 <- TripService.createTrip("Trip B", startDate.plusDays(10), endDate.plusDays(10), ownerId, xa)
      trip3 <- TripService.createTrip("Trip C", startDate.plusDays(20), endDate.plusDays(20), ownerId, xa)
    } yield {
      assert(trip1.ownerId == ownerId)
      assert(trip2.ownerId == ownerId)
      assert(trip3.ownerId == ownerId)
      assert(trip1.id != trip2.id && trip2.id != trip3.id)
    }
  }

  test("Create trip - should handle special characters in title") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val specialTitle = "My Trip & Friends (2024) - Vacation!"
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(5)

    for {
      _ <- setupSchema(xa)
      result <- TripService.createTrip(specialTitle, startDate, endDate, ownerId, xa)
    } yield {
      assert(result.title == specialTitle)
    }
  }

  // ===== GET TRIP BY ID TESTS =====

  test("Get trip by ID - should retrieve existing trip") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(5)

    for {
      _ <- setupSchema(xa)
      created <- TripService.createTrip("Test Trip", startDate, endDate, ownerId, xa)
      retrieved <- TripService.getTripById(created.id, xa)
    } yield {
      assert(retrieved.isDefined, "Should find created trip")
      assert(retrieved.get.id == created.id)
      assert(retrieved.get.title == "Test Trip")
    }
  }

  test("Get trip by ID - should return None for non-existent trip") {
    val xa = databaseFixture()
    val nonExistentId = UUID.randomUUID()

    for {
      _ <- setupSchema(xa)
      retrieved <- TripService.getTripById(nonExistentId, xa)
    } yield {
      assert(retrieved.isEmpty, "Should not find non-existent trip")
    }
  }

  test("Get trip by ID - should return correct trip when multiple exist") {
    val xa = databaseFixture()
    val owner1 = UUID.randomUUID()
    val owner2 = UUID.randomUUID()
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(5)

    for {
      _ <- setupSchema(xa)
      trip1 <- TripService.createTrip("Trip 1", startDate, endDate, owner1, xa)
      trip2 <- TripService.createTrip("Trip 2", startDate, endDate, owner2, xa)
      trip3 <- TripService.createTrip("Trip 3", startDate, endDate, owner1, xa)
      retrieved <- TripService.getTripById(trip2.id, xa)
    } yield {
      assert(retrieved.isDefined)
      assert(retrieved.get.title == "Trip 2")
      assert(retrieved.get.ownerId == owner2)
    }
  }
  // ===== UPDATE TRIP TESTS =====

  test("Update trip - should update title") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(5)

    for {
      _ <- setupSchema(xa)
      created <- TripService.createTrip("Original Title", startDate, endDate, ownerId, xa)
      _ <- TripService.updateTrip(created.id, Some("Updated Title"), None, None, xa)
      updated <- TripService.getTripById(created.id, xa)
    } yield {
      assert(updated.isDefined)
      assert(updated.get.title == "Updated Title", "Title should be updated")
      assert(updated.get.startDate == startDate, "Start date should remain unchanged")
    }
  }

  test("Update trip - should update start date") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val originalStart = LocalDate.of(2024, 6, 1)
    val originalEnd = LocalDate.of(2024, 6, 10)
    val newStart = LocalDate.of(2024, 7, 1)

    for {
      _ <- setupSchema(xa)
      created <- TripService.createTrip("Trip", originalStart, originalEnd, ownerId, xa)
      _ <- TripService.updateTrip(created.id, None, Some(newStart), None, xa)
      updated <- TripService.getTripById(created.id, xa)
    } yield {
      assert(updated.isDefined)
      assert(updated.get.startDate == newStart, "Start date should be updated")
      assert(updated.get.endDate == originalEnd, "End date should remain unchanged")
    }
  }

  test("Update trip - should update end date") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val originalStart = LocalDate.of(2024, 6, 1)
    val originalEnd = LocalDate.of(2024, 6, 10)
    val newEnd = LocalDate.of(2024, 6, 20)

    for {
      _ <- setupSchema(xa)
      created <- TripService.createTrip("Trip", originalStart, originalEnd, ownerId, xa)
      _ <- TripService.updateTrip(created.id, None, None, Some(newEnd), xa)
      updated <- TripService.getTripById(created.id, xa)
    } yield {
      assert(updated.isDefined)
      assert(updated.get.endDate == newEnd, "End date should be updated")
      assert(updated.get.startDate == originalStart, "Start date should remain unchanged")
    }
  }

  test("Update trip - should update all fields") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val originalStart = LocalDate.of(2024, 6, 1)
    val originalEnd = LocalDate.of(2024, 6, 10)
    val newStart = LocalDate.of(2024, 7, 1)
    val newEnd = LocalDate.of(2024, 7, 15)

    for {
      _ <- setupSchema(xa)
      created <- TripService.createTrip("Original", originalStart, originalEnd, ownerId, xa)
      _ <- TripService.updateTrip(created.id, Some("New Title"), Some(newStart), Some(newEnd), xa)
      updated <- TripService.getTripById(created.id, xa)
    } yield {
      assert(updated.isDefined)
      assert(updated.get.title == "New Title")
      assert(updated.get.startDate == newStart)
      assert(updated.get.endDate == newEnd)
      assert(updated.get.ownerId == ownerId, "Owner ID should not change")
    }
  }

  test("Update trip - should not update non-existent trip") {
    val xa = databaseFixture()
    val nonExistentId = UUID.randomUUID()

    for {
      _ <- setupSchema(xa)
      _ <- TripService.updateTrip(nonExistentId, Some("New Title"), None, None, xa)
      result <- TripService.getTripById(nonExistentId, xa)
    } yield {
      assert(result.isEmpty, "Non-existent trip should not be created")
    }
  }

  test("Update trip - should handle None for all fields (no-op)") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.of(2024, 6, 1)
    val endDate = LocalDate.of(2024, 6, 10)

    for {
      _ <- setupSchema(xa)
      created <- TripService.createTrip("Original", startDate, endDate, ownerId, xa)
      _ <- TripService.updateTrip(created.id, None, None, None, xa)
      updated <- TripService.getTripById(created.id, xa)
    } yield {
      assert(updated.isDefined)
      assert(updated.get.title == "Original", "Should not change when all None")
      assert(updated.get.startDate == startDate)
      assert(updated.get.endDate == endDate)
    }
  }

  test("Update trip - should handle empty string for title") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(5)

    for {
      _ <- setupSchema(xa)
      created <- TripService.createTrip("Original", startDate, endDate, ownerId, xa)
      _ <- TripService.updateTrip(created.id, Some(""), None, None, xa)
      updated <- TripService.getTripById(created.id, xa)
    } yield {
      assert(updated.isDefined)
      assert(updated.get.title == "", "Should allow empty string")
    }
  }

  // ===== DELETE TRIP TESTS =====

  test("Delete trip - should delete existing trip") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.now()
    val endDate = startDate.plusDays(5)

    for {
      _ <- setupSchema(xa)
      created <- TripService.createTrip("Trip to Delete", startDate, endDate, ownerId, xa)
      _ <- TripService.deleteTrip(created.id, xa)
      deleted <- TripService.getTripById(created.id, xa)
    } yield {
      assert(deleted.isEmpty, "Trip should be deleted")
    }
  }

  test("Delete trip - should not fail when deleting non-existent trip") {
    val xa = databaseFixture()
    val nonExistentId = UUID.randomUUID()

    for {
      _ <- setupSchema(xa)
      _ <- TripService.deleteTrip(nonExistentId, xa)
      result <- TripService.getTripById(nonExistentId, xa)
    } yield {
      assert(result.isEmpty, "Should handle deleting non-existent trip gracefully")
    }
  }

  // ===== INTEGRATION TESTS =====

  test("Integration - full CRUD cycle") {
    val xa = databaseFixture()
    val ownerId = UUID.randomUUID()
    val startDate = LocalDate.of(2024, 6, 1)
    val endDate = LocalDate.of(2024, 6, 10)

    for {
      _ <- setupSchema(xa)
      // Create
      created <- TripService.createTrip("My Trip", startDate, endDate, ownerId, xa)
      // Read
      retrieved <- TripService.getTripById(created.id, xa)
      // Update
      _ <- TripService.updateTrip(created.id, Some("Updated Trip"), None, None, xa)
      updated <- TripService.getTripById(created.id, xa)
      // Delete
      _ <- TripService.deleteTrip(created.id, xa)
      deleted <- TripService.getTripById(created.id, xa)
    } yield {
      assert(retrieved.isDefined && retrieved.get.title == "My Trip")
      assert(updated.isDefined && updated.get.title == "Updated Trip")
      assert(deleted.isEmpty)
    }
  }




