package ff.messages

case class Velocity(
                     raceTrackId: String,
                     timeStamp: Int,
                     velocity: Double,
                     t: Int
                   ) extends Message
