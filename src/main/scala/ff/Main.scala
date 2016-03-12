package ff

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.slf4j.LazyLogging
import io.scalac.amqp.Connection

object Main extends App with LazyLogging {

  implicit val system = ActorSystem("ff")
  implicit val materializer = ActorMaterializer()

  val id = "FlashAndFurious"
  val accessCode = "zajorsida"

  val START_TEMPLATE = s"/topic/pilots/$id/start"
  val STOP_TEMPLATE = s"/topic/pilots/$id/stop"
  val SENSOR_TEMPLATE = s"/topic/pilots/$id/sensor"
  val VELOCITY_TEMPLATE = s"/topic/pilots/$id/velocity"
  val PENALTY_TEMPLATE = s"/topic/pilots/$id/penalty"
  val ROUND_PASSED_TEMPLATE = s"/topic/pilots/$id/roundtime"
  val ANNOUNCE = s"/app/pilots/announce"
  val POWER = s"/app/pilots/power"

  // streaming invoices to Accounting Department
  val connection = Connection()

  // create org.reactivestreams.Publisher
  val queue = connection.consume(queue = POWER)

  // create org.reactivestreams.Subscriber
  val exchange = connection.publish(exchange = "accounting_department", routingKey = "invoices")

  // Run akka-streams with queue as Source and exchange as Sink
  Source
    .fromPublisher(queue)
    .map { x =>

      println(ByteString.fromArray(x.message.body.toArray).utf8String)
      x.message
    }
    .runWith(Sink.ignore)



  //Sink.fromSubscriber(exchange))

}