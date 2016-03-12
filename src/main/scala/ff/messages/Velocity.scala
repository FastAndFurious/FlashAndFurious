package ff.messages

case class Velocity(
                     racetrackId: String,
                     timeStamp: Int,
                     velocity: Double,
                     t: Int
                   ) extends Message
