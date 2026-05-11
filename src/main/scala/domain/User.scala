package domain

import java.util.UUID

case class User(
                 id: UUID,
                 username: String,
                 passwordHash: String
               )