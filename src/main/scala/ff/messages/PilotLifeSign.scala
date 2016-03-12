package ff.messages

case class PilotLifeSign(
                        teamId: String,
                        accessCode: String
                        ) extends Message
