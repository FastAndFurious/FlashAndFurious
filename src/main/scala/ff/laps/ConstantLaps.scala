package ff.laps

import akka.actor.Props
import ff.Main
import ff.messages.RaceStop

class ConstantLaps(pow: Int) extends Lapper {

  def running: Receive = {
    case x if x != RaceStop =>
      Main.emitPower(pow)
  }

}

object ConstantLaps {

  def props(pow: Int): Props = Props(classOf[ConstantLaps], pow)

}
