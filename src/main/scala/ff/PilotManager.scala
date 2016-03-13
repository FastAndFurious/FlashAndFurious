package ff

import akka.actor.{Actor, ActorLogging, Props}
import ff.filters._
import ff.messages._
import ff.tokens._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer

import scala.language.postfixOps

import scala.concurrent.duration._

class PilotManager(emitPower: Int => Unit) extends Actor with ActorLogging {

  var CruiseSpeed = 120 // safe speed
  val CurveSpeed = 125 // boost on curve

  val MaxSpeed = 200

  val InitialTokensToIgnore = 2
  val TokensNeeded = 55
  val TokenSignatureLength = 10

  val smoother: StatefulFilter = new Kalman(4000, 1, 4, 0.5, 0)

  def correctGyroZ(gyroZ: Int): Int = (smoother(gyroZ) / 800).toInt

  override def receive: Receive = waitOnStart

  var curLap = 0
  var curDirection = 0
  var curTimestamp = 0L

  var history = ArrayBuffer[Token]()

  def cleanup(): Unit = {
    curDirection = 0
    curTimestamp = 0L
    curLap = 0
    history.clear()
  }

  final def waitOnStart: Receive = {
    case RaceStart =>
      cleanup()
      emitPower(CruiseSpeed)
      context.become(explorer)
    case m =>
      log.warning(s"RaceStart expected: received $m")
  }

  final def fallbackHandler: Receive = {
    case RaceStop => context.become(waitOnStart)
    case _ : RoundTime => curLap += 1
      CruiseSpeed += 1
    case _ : Velocity | Power => /* ignore */
    case _ : Penalty => emitPower(CruiseSpeed)
      CruiseSpeed = (CruiseSpeed - 1) max 120
    case m @ _ =>
      log.warning(s"RaceStop expected: received $m")
  }

  // Explore the circuit
  final def explorer: Receive = {
    case Sensor(_, t, _, gyro, _, _) => {

      //val t = System.currentTimeMillis()

      val gyroZ = correctGyroZ(gyro._3)
      val direction = Integer.signum(gyroZ)

      if (direction != curDirection) {
        val duration = t - curTimestamp

        val token = {
          if (curDirection > 0) RightTurn(duration)
          else if (curDirection < 0) LeftTurn(duration)
          else Straight(duration)
        }

        history += token

        if (direction == 0 && history.size > TokensNeeded) {

          // a straight road detected
          // go berseker?

          val predictedDuration = predictStraightDuration()

          if (predictedDuration > 300) {
            emitPower(MaxSpeed)
            context.system.scheduler.scheduleOnce((0.25 * predictedDuration) millis) {
              emitPower(CruiseSpeed)
            }
          }
        }

        if (direction != 0) {
          emitPower(CurveSpeed)
          context.system.scheduler.scheduleOnce(100 millis) {
            emitPower(CruiseSpeed)
          }
        }

        curTimestamp = t
        curDirection = direction
      }
    }
    case m => fallbackHandler(m)
  }

  def predictStraightDuration(): Long = {
    val pattern = history.takeRight(TokenSignatureLength)
    val text = history.take(TokensNeeded)

    var mostMatches = 0
    var result = 0L

    var i = 0
    val ibound = text.size - pattern.size
    while (i < ibound) {
      val candidate = text(i + pattern.size)
      if (candidate.isInstanceOf[Straight]) {
        var j = 0
        var jbound = pattern.size

        var matches = 0
        while (j < jbound) {
          if (history(i + j).sameTypeAs(pattern(j)))
            matches += 1
          j += 1
        }

        if (matches > mostMatches) {
          mostMatches = matches
          result = candidate.duration
        }
      }

      i += 1
    }

    if (mostMatches >= 8)
      result
    else
      0
  }
}

object PilotManager {
  def props(emitPower: Int => Unit): Props = Props(classOf[PilotManager], emitPower)
}


