package domain

import java.util.UUID
import java.time.LocalDate
import io.circe.Codec
import io.circe.generic.semiauto.*

case class Trip(
                 id: UUID,
                 title: String,
                 startDate: LocalDate,
                 endDate: LocalDate,
                 ownerId: UUID
                )

object Trip:
  implicit val codec: Codec[Trip] = deriveCodec
