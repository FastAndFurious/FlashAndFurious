package ff.messages

case class Power(
                teamId: String,
                accessCode: String,
                p: Int
                ) extends Message
