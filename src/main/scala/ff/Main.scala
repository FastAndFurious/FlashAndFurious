package ff

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.scalac.amqp.Connection

object Main extends App with LazyLogging {

  implicit val system = ActorSystem("ff")
  implicit val materializer = ActorMaterializer()

  // streaming invoices to Accounting Department
  val connection = Connection()

  // create org.reactivestreams.Publisher
  val queue = connection.consume(queue = "invoices")

  // create org.reactivestreams.Subscriber
  val exchange = connection.publish(exchange = "accounting_department", routingKey = "invoices")

  // Run akka-streams with queue as Source and exchange as Sink
  Source
    .fromPublisher(queue)
    .map { x =>
      println(x)
      x.message
    }
    .runWith(Sink.fromSubscriber(exchange))

}