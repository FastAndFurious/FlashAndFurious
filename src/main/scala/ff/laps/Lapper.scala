package ff.laps

import akka.actor.{Actor, ActorLogging}
import ff.messages.{RaceStart, RaceStop}

trait Lapper extends Actor with ActorLogging {

  final override def receive: Receive = waitOnStart

  def running: Receive

  final def waitOnStart: Receive = {
    case RaceStart =>
      context.become(waitOnStop)
      log.info(s"race started")
    case x =>
      log.warning(s"race start expected, got: $x")
  }

  final def waitOnStop: Receive = running orElse {
    case RaceStop =>
      context.become(waitOnStart)
      log.info(s"race stopped")
    case x =>
      log.warning(s"race stop expected, got: $x")
  }

}
