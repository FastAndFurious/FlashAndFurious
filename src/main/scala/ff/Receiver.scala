package ff

import akka.actor.{Actor, ActorLogging, Props}

class Receiver(emitPower: Int => Unit) extends Actor with ActorLogging {

  override def receive: Receive = {

    case x =>

  }

}

object Receiver {

  def props(emitPower: Int => Unit): Props = Props(classOf[Receiver], emitPower)

}
