package ff.messages

case class Penalty(
                    raceTrack: String,
                    actualSpeed: Double,
                    speedLimit: Double,
                    penalty_ms: Double,
                    barrier: Option[Int]
                  ) extends Message
