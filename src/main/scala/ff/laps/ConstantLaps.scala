package ff.laps

import akka.actor.{Actor, ActorLogging, Props}
import ff.Main
import ff.messages.{RaceStart, RaceStop}

class ConstantLaps(pow: Int) extends Actor with ActorLogging {

  final override def receive: Receive = waitOnStart

  final def waitOnStart: Receive = {
    case RaceStart =>
      context.become(waitOnStop)
      log.info(s"race started")
    case x =>
      log.warning(s"race start expected, got: $x")
  }

  final def waitOnStop: Receive = {
    case x if x != RaceStop =>
      Main.emitPower(pow)

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
