package ff.tokens

sealed trait Token {

  def duration: Long

  def sameTypeAs(that: Token): Boolean = (this, that) match {
    case (_ : Straight, _ : Straight) |
         (_ : LeftTurn, _ : LeftTurn) |
         (_ : RightTurn, _ : RightTurn) => true
    case _ => false
  }

}

case class Straight(val duration: Long) extends Token
case class LeftTurn(val duration: Long) extends Token
case class RightTurn(val duration: Long) extends Token
