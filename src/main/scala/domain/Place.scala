package domain

import java.util.UUID
import java.time.OffsetDateTime
import io.circe.Codec
import io.circe.generic.semiauto.*

case class Place(
                  id: UUID,
                  tripId: UUID,
                  name: String,
                  description: Option[String],
                  lat: BigDecimal,
                  lng: BigDecimal,
                  startDate: OffsetDateTime,
                  endDate: OffsetDateTime
                )

object Place:
  implicit val codec: Codec[Place] = deriveCodec
