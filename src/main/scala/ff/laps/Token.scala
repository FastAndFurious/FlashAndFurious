package ff.tokens

import ff.tokens.Dir.Dir

/**
  * Created by mukel on 3/12/16.
  */

object Dir extends Enumeration{
    type Dir = Value
    val straight, left, right = Value
}

trait Token {
  val duration: Double
  val direction: Dir
}

case class Straight(val duration: Double) extends Token { val direction = Dir.straight }
case class LeftTurn(val duration: Double, steepness: Double) extends Token { val direction = Dir.left }
case class RightTurn(val duration: Double, steepness: Double) extends Token { val direction = Dir.right }

