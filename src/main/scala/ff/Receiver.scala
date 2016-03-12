package ff

import akka.actor.{Actor, ActorLogging, Props}

class Receiver(emitPower: Int => Unit) extends Actor with ActorLogging {

  var count = 1

  override def receive: Receive = {

    case x =>
      count += 1
      emitPower(count % 40)
      println(x.toString)

  }

}

object Receiver {

  def props(emitPower: Int => Unit): Props = Props(classOf[Receiver], emitPower)

}
