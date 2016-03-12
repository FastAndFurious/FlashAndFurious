package ff.messages

case class Power(
                teamId: String,
                accessCode: String,
                p: Int,
                timestamp: Int
                ) extends Message
