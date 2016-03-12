package ff.messages

case class RoundTime(
               track: String,
               team: String,
               roundDuration: Int,
               timestamp: Int
               ) extends Message
