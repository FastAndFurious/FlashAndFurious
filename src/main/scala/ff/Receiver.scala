package ff

import akka.actor.Props
import akka.stream.actor._

class Receiver extends ActorSubscriber {

  import ActorSubscriberMessage._

  override def requestStrategy =
    OneByOneRequestStrategy

  override def receive: Receive = {

    case OnNext(x) =>
      println(x)

  }

}

object Receiver {

  def props: Props = Props(new Receiver)

}
