package domain

import java.util.UUID
import java.time.OffsetDateTime

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