package domain

import java.util.UUID

case class TripParticipant(
                            id: UUID,
                            tripId: UUID,
                            userId: UUID
                          )