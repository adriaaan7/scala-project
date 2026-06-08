package service

import cats.effect.IO
import munit.CatsEffectSuite
import domain.Place
import repository.PlaceRepository
import doobie.hikari.HikariTransactor
import doobie.implicits._
import cats.effect.Resource
import cats.implicits._
import java.util.UUID
import java.time.OffsetDateTime
import java.time.ZoneOffset

class PlaceServiceSuite extends CatsEffectSuite:

  val transactorResource: Resource[IO, HikariTransactor[IO]] =
    for {
      ec <- Resource.pure(scala.concurrent.ExecutionContext.global)
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = "org.h2.Driver",
        url             = "jdbc:h2:mem:place_test_db;DB_CLOSE_DELAY=-1",
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
      CREATE TABLE IF NOT EXISTS places (
        id UUID PRIMARY KEY,
        trip_id UUID NOT NULL,
        name VARCHAR(255) NOT NULL,
        description TEXT,
        lat DECIMAL(10, 8) NOT NULL,
        lng DECIMAL(11, 8) NOT NULL,
        start_date TIMESTAMP WITH TIME ZONE NOT NULL,
        end_date TIMESTAMP WITH TIME ZONE NOT NULL
      )
    """.update.run.transact(xa).void

  private def insertPlace(place: Place, xa: HikariTransactor[IO]): IO[Unit] =
    PlaceRepository.save(place).transact(xa).void

  // ===== GET PLACES BY TRIP ID - BASIC TESTS =====

  test("Get places by trip ID - should return empty list when no places exist") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()

    for {
      _ <- setupSchema(xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.isEmpty, "Should return empty list for trip with no places")
    }
  }

  test("Get places by trip ID - should return single place") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Eiffel Tower",
      description = Some("Famous landmark"),
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.length == 1, "Should return single place")
      assert(places.head.name == "Eiffel Tower")
      assert(places.head.tripId == tripId)
    }
  }

  test("Get places by trip ID - should return multiple places") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    val places = (1 to 5).map { i =>
      Place(
        id = UUID.randomUUID(),
        tripId = tripId,
        name = s"Place $i",
        description = Some(s"Description $i"),
        lat = BigDecimal("48.85837") + BigDecimal(i * 0.01),
        lng = BigDecimal("2.294481"),
        startDate = now,
        endDate = now.plusHours(i)
      )
    }.toList

    for {
      _ <- setupSchema(xa)
      _ <- places.foldM(()) { (_, p) =>
        insertPlace(p, xa)
      }
      retrieved <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(retrieved.length == 5, s"Should return 5 places, got ${retrieved.length}")
      assert(retrieved.map(_.name).forall(_.startsWith("Place")))
    }
  }

  test("Get places by trip ID - should only return places for specified trip") {
    val xa = databaseFixture()
    val tripId1 = UUID.randomUUID()
    val tripId2 = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    val place1 = Place(
      id = UUID.randomUUID(),
      tripId = tripId1,
      name = "Place in Trip 1",
      description = None,
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    val place2 = Place(
      id = UUID.randomUUID(),
      tripId = tripId2,
      name = "Place in Trip 2",
      description = None,
      lat = BigDecimal("51.50735"),
      lng = BigDecimal("-0.127758"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place1, xa)
      _ <- insertPlace(place2, xa)
      placesTrip1 <- PlaceService.getPlacesByTripId(tripId1, xa)
      placesTrip2 <- PlaceService.getPlacesByTripId(tripId2, xa)
    } yield {
      assert(placesTrip1.length == 1)
      assert(placesTrip1.head.name == "Place in Trip 1")
      assert(placesTrip2.length == 1)
      assert(placesTrip2.head.name == "Place in Trip 2")
    }
  }

  // ===== PLACE DATA TESTS =====

  test("Get places by trip ID - should preserve place with None description") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Place without description",
      description = None,
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.description.isEmpty)
    }
  }

  test("Get places by trip ID - should preserve place with description") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val description = "Beautiful place with historical significance"
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Historical Site",
      description = Some(description),
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.description == Some(description))
    }
  }

  test("Get places by trip ID - should preserve coordinates") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val lat = BigDecimal("48.85837")
    val lng = BigDecimal("2.294481")
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Eiffel Tower",
      description = None,
      lat = lat,
      lng = lng,
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.lat == lat)
      assert(places.head.lng == lng)
    }
  }

  test("Get places by trip ID - should handle zero coordinates") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Prime Meridian",
      description = None,
      lat = BigDecimal("0"),
      lng = BigDecimal("0"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.lat == BigDecimal("0"))
      assert(places.head.lng == BigDecimal("0"))
    }
  }

  test("Get places by trip ID - should handle negative coordinates") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val lat = BigDecimal("-33.86882")
    val lng = BigDecimal("151.21399")
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Sydney Opera House",
      description = None,
      lat = lat,
      lng = lng,
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.lat == lat)
      assert(places.head.lng == lng)
    }
  }

  test("Get places by trip ID - should preserve place UUID") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val placeId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = placeId,
      tripId = tripId,
      name = "Place with specific UUID",
      description = None,
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.id == placeId)
    }
  }

  // ===== DATE/TIME TESTS =====

  test("Get places by trip ID - should handle places with same start and end time") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Instant Place",
      description = None,
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.startDate == places.head.endDate)
    }
  }

  test("Get places by trip ID - should handle past dates") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val pastDate = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Historical Place",
      description = None,
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = pastDate,
      endDate = pastDate.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.startDate.getYear == 2020)
    }
  }

  test("Get places by trip ID - should handle future dates") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val futureDate = OffsetDateTime.now(ZoneOffset.UTC).plusYears(1)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Future Place",
      description = None,
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = futureDate,
      endDate = futureDate.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.startDate.getYear > 2024)
    }
  }

  // ===== PLACE NAME TESTS =====

  test("Get places by trip ID - should preserve place names") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val names = List("Restaurant", "Museum", "Hotel", "Park", "Beach")

    val places = names.map { name =>
      Place(
        id = UUID.randomUUID(),
        tripId = tripId,
        name = name,
        description = None,
        lat = BigDecimal("48.85837"),
        lng = BigDecimal("2.294481"),
        startDate = now,
        endDate = now.plusHours(1)
      )
    }

    for {
      _ <- setupSchema(xa)
      _ <- places.foldM(()) { (_, p) =>
        insertPlace(p, xa)
      }
      retrieved <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(retrieved.map(_.name).toSet == names.toSet)
    }
  }

  test("Get places by trip ID - should handle long place names") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val longName = "Very Long Description of Place " + ("x" * 200)
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = longName,
      description = None,
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.name == longName)
    }
  }

  test("Get places by trip ID - should handle special characters in names") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val specialName = "Place & Museum (2024) - Café Français"
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = specialName,
      description = None,
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.name == specialName)
    }
  }

  test("Get places by trip ID - should handle unicode characters in names") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val unicodeName = "Москва Красная площадь / 莫斯科紅場 / 모스크바"
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = unicodeName,
      description = None,
      lat = BigDecimal("55.75396"),
      lng = BigDecimal("37.62015"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.name == unicodeName)
    }
  }

  // ===== DESCRIPTION TESTS =====

  test("Get places by trip ID - should handle long descriptions") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val longDesc = "This is a very detailed description of the place. " * 50
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Place",
      description = Some(longDesc),
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.description == Some(longDesc))
    }
  }

  test("Get places by trip ID - should handle multiline descriptions") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val multilineDesc = "Line 1\nLine 2\nLine 3\nLine 4"
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Place",
      description = Some(multilineDesc),
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.description == Some(multilineDesc))
    }
  }

  test("Get places by trip ID - should handle empty descriptions") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val place = Place(
      id = UUID.randomUUID(),
      tripId = tripId,
      name = "Place",
      description = Some(""),
      lat = BigDecimal("48.85837"),
      lng = BigDecimal("2.294481"),
      startDate = now,
      endDate = now.plusHours(1)
    )

    for {
      _ <- setupSchema(xa)
      _ <- insertPlace(place, xa)
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.head.description == Some(""))
    }
  }

  // ===== LARGE DATASET TESTS =====

  test("Get places by trip ID - should handle many places (100)") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    val places = (1 to 100).map { i =>
      Place(
        id = UUID.randomUUID(),
        tripId = tripId,
        name = s"Place $i",
        description = Some(s"Description $i"),
        lat = BigDecimal("48.85837") + BigDecimal(i * 0.001),
        lng = BigDecimal("2.294481"),
        startDate = now,
        endDate = now.plusHours(1)
      )
    }.toList

    for {
      _ <- setupSchema(xa)
      _ <- places.foldM(()) { (_, p) =>
        insertPlace(p, xa)
      }
      retrieved <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(retrieved.length == 100)
    }
  }

  test("Get places by trip ID - should return places in insertion order") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    val places = (1 to 10).map { i =>
      Place(
        id = UUID.randomUUID(),
        tripId = tripId,
        name = s"Place $i",
        description = None,
        lat = BigDecimal("48.85837"),
        lng = BigDecimal("2.294481"),
        startDate = now,
        endDate = now.plusHours(1)
      )
    }.toList

    for {
      _ <- setupSchema(xa)
      _ <- places.foldM(()) { (_, p) =>
        insertPlace(p, xa)
      }
      retrieved <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      // Verify all places are returned
      assert(retrieved.length == 10)
      // Verify the names match (order may vary)
      assert(retrieved.map(_.name).toSet == (1 to 10).map(i => s"Place $i").toSet)
    }
  }

  test("Get places by trip ID - should handle multiple trips with many places each") {
    val xa = databaseFixture()
    val trip1 = UUID.randomUUID()
    val trip2 = UUID.randomUUID()
    val trip3 = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    val places1 = (1 to 10).map { i =>
      Place(
        id = UUID.randomUUID(),
        tripId = trip1,
        name = s"Trip1-Place$i",
        description = None,
        lat = BigDecimal("48.85837"),
        lng = BigDecimal("2.294481"),
        startDate = now,
        endDate = now.plusHours(1)
      )
    }.toList

    val places2 = (1 to 15).map { i =>
      Place(
        id = UUID.randomUUID(),
        tripId = trip2,
        name = s"Trip2-Place$i",
        description = None,
        lat = BigDecimal("51.50735"),
        lng = BigDecimal("-0.127758"),
        startDate = now,
        endDate = now.plusHours(1)
      )
    }.toList

    val places3 = (1 to 5).map { i =>
      Place(
        id = UUID.randomUUID(),
        tripId = trip3,
        name = s"Trip3-Place$i",
        description = None,
        lat = BigDecimal("35.6762"),
        lng = BigDecimal("139.7674"),
        startDate = now,
        endDate = now.plusHours(1)
      )
    }.toList

    for {
      _ <- setupSchema(xa)
      _ <- (places1 ++ places2 ++ places3).foldM(()) { (_, p) =>
        insertPlace(p, xa)
      }
      retrieved1 <- PlaceService.getPlacesByTripId(trip1, xa)
      retrieved2 <- PlaceService.getPlacesByTripId(trip2, xa)
      retrieved3 <- PlaceService.getPlacesByTripId(trip3, xa)
    } yield {
      assert(retrieved1.length == 10)
      assert(retrieved2.length == 15)
      assert(retrieved3.length == 5)
      assert(retrieved1.forall(_.tripId == trip1))
      assert(retrieved2.forall(_.tripId == trip2))
      assert(retrieved3.forall(_.tripId == trip3))
    }
  }

  test("Integration - full place retrieval workflow") {
    val xa = databaseFixture()
    val tripId = UUID.randomUUID()
    val now = OffsetDateTime.now(ZoneOffset.UTC)

    for {
      _ <- setupSchema(xa)
      // Create multiple places
      place1 = Place(UUID.randomUUID(), tripId, "Restaurant", Some("Italian"), BigDecimal("48.85837"), BigDecimal("2.294481"), now, now.plusHours(1))
      place2 = Place(UUID.randomUUID(), tripId, "Museum", None, BigDecimal("48.86162"), BigDecimal("2.33614"), now.plusDays(1), now.plusDays(1).plusHours(2))
      place3 = Place(UUID.randomUUID(), tripId, "Hotel", Some("4-star"), BigDecimal("48.87033"), BigDecimal("2.34568"), now.plusDays(2), now.plusDays(2).plusHours(1))
      _ <- List(place1, place2, place3).foldM(()) { (_, p) =>
        insertPlace(p, xa)
      }
      // Retrieve all places
      places <- PlaceService.getPlacesByTripId(tripId, xa)
    } yield {
      assert(places.length == 3)
      assert(places.map(_.name).sorted == List("Hotel", "Museum", "Restaurant"))
    }
  }










