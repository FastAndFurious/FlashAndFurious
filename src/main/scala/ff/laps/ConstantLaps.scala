package ff.laps

import akka.actor.{Actor, ActorLogging, Props}
import ff.Main
import ff.messages._

import scala.collection.mutable

class ConstantLaps(var pow: Int) extends Actor with ActorLogging {
  val looper = mutable.ListBuffer[Int]()
  var max_pow = 140

  final override def receive: Receive = waitOnStart

  final def waitOnStart: Receive = {
    case RaceStart =>
      context.become(waitOnStop)
      log.info(s"race started")


    case x =>
      log.warning(s"race start expected, got: $x")
  }

  final def waitOnStop: Receive = {

    case s@Sensor(_,_,_,g,_,t) =>
      if (looper.length > 10 && math.abs(looper(looper.length - 10)) > 3000 && math.abs(g._3) < 3000) {
        Main.emitPower(255)
      } else {
        Main.emitPower(pow)
      }
      log.info(pow.toString)
      looper += g._3

      s match {
        case Sensor(_, ts, (a1, a2, a3), (g1, g2, g3), (m1, m2, m3), t) =>
          log.info(s"$ts $a1 $a2 $a3 $g1 $g2 $g3 $m1 $m2 $m3 $t")
      }


    case p: Penalty =>
      pow -= 10
      max_pow = pow
      Main.emitPower(pow)

    case r: RoundTime =>
      pow += 10
      if (pow > max_pow) pow = max_pow


    case RaceStop =>
      context.become(waitOnStart)
      log.info(s"race stopped")
    case x =>
      log.warning(s"race stop expected, got: $x")
  }

}

object ConstantLaps {

  def props(pow: Int): Props = Props(classOf[ConstantLaps], pow)

}
