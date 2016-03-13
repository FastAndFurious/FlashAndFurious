package ff.tokens

/**
  * Created by mukel on 3/12/16.
  */
trait Token {
  def duration: Long

  def sameTypeAs(that: Token): Boolean = (this, that) match {
    case (_ : Straight, _ : Straight) |
         (_ : LeftTurn, _ : LeftTurn) |
         (_ : RightTurn, _ : RightTurn) => true
    case _ => false
  }

}
//trait Unknown extends Token
case class Straight(val duration: Long) extends Token
case class LeftTurn(val duration: Long) extends Token
case class RightTurn(val duration: Long) extends Token
