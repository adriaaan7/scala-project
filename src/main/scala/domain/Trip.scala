package domain

import java.util.UUID
import java.time.LocalDate

case class Trip(
                 id: UUID,
                 title: String,
                 startDate: LocalDate,
                 endDate: LocalDate,
                 ownerId: UUID
               )