package ff.messages

case class Power(
                teamId: String,
                accessCode: String,
                p: Int
                ) extends Message {

  require(0 <= p && p <= 255, "power should be between 0 and 255")

}
