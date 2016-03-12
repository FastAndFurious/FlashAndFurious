package ff.laps

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor}
import akka.stream.actor.ActorSubscriberMessage.OnNext
import ff.Main
import ff.messages._

import scala.collection.mutable

class AccelerationLaps(var basePower: Int) extends Actor with ActorLogging {

  override def receive: Receive = waitOnStart

  final def waitOnStart: Receive = {
    case RaceStart =>
      context.become(waitOnStop)
      log.info(s"race started")

    case x =>
      log.warning(s"race start expected, got: $x ${sender()}")
  }

  /**
    * More you are on a straight line, the sooner you will find a turn
    */
  var minSpeed = 140
  var decreaseCoeff = 0.3
  var timeSpentInStraight = 0
  var m = mutable.ListBuffer.fill(5)(0)
  val bound = 400
  var powerToApply = 250

  final def waitOnStop: Receive = {

    case s@Sensor(_, _, _, g, _, t) =>

      /** Check if we are in a straight line */
      m.tail += g._3

      if (Math.abs(m.sum / m.length) < bound) {
        // we probably are in a straight line
        if (timeSpentInStraight == 0)
          powerToApply = 255
        else {
          if (255 - timeSpentInStraight < minSpeed)
            powerToApply = minSpeed
          else
            powerToApply = 255 - timeSpentInStraight
        }

        timeSpentInStraight += 1
      } else {
        // we are not in a straight line
        timeSpentInStraight = 0
      }

      Main.emitPower(powerToApply)


      s match {
        case Sensor(_, ts, (a1, a2, a3), (g1, g2, g3), (m1, m2, m3), t) =>
          log.info(s"$ts $a1 $a2 $a3 $g1 $g2 $g3 $m1 $m2 $m3 $t")
      }

    case Velocity(_, _, _, _) =>
    case RaceStop =>
      context.become(waitOnStart)
      log.info(s"race stopped")

    case x =>
      log.warning(s"race stop expected, got: $x")
  }

}


object AccelerationLaps {

  def props(basePow: Int): Props = Props(classOf[AccelerationLaps], basePow)

}
