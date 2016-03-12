package ff

import akka.actor.{Actor, ActorLogging, Props}

import scala.collection.mutable.ListBuffer

//import ff.filters.GyroZCorrector
import ff.messages.Sensor
import ff.tokens.{Token, LeftTurn, RightTurn, Straight}

import scala.collection.mutable

class Tokenizer() {
  var prevSign = 0
  var lastSignChange = 0
  var loop = mutable.ListBuffer[Token]()
  var steer = 0

  def tokenize(g_z: Int, t: Int, max: Int): Token = {
    val gyroZ = g_z/max
    val curSign = Integer.signum(gyroZ)

    if (lastSignChange == 0)
      lastSignChange = t
    // changing sign <=> changing road shape
    // 0 straight
    // -1 left turn
    // +1 right turn

    val duration =   t - lastSignChange

    if (prevSign != curSign) {

      if (prevSign < 0)
        loop += LeftTurn(duration, steer)
      else if (prevSign > 0)
        loop += RightTurn(duration, steer)
      else
        loop += Straight(duration)
      lastSignChange = t
      steer = 0
    } else {
      if (g_z > steer)
        steer = g_z
    }
    prevSign = curSign


    if (curSign < 0)
      LeftTurn(duration, steer)
    else if (prevSign > 0)
      RightTurn(duration, steer)
    else
      Straight(duration)
  }

  def getLoop(): ListBuffer[Token] = {
    val retLoop = loop
    loop = new ListBuffer[Token]()
    retLoop
  }
}
