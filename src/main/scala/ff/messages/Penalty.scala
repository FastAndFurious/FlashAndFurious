package ff.messages

case class Penalty(
                    raceTrack: String,
                    actualSpeed: Double,
                    speedLimit: Double,
                    penalty_ms: Double,
                    barrier: String
                  ) extends Message
