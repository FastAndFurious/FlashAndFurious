package ff

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.scalalogging.slf4j.LazyLogging
import ff.messages._
import io.scalac.amqp.{Connection, Delivery}
import spray.json.DefaultJsonProtocol._
import spray.json._

object Main extends App with LazyLogging {

  implicit val system = ActorSystem("ff")
  implicit val materializer = ActorMaterializer()

  // out
  implicit val powerFormat = jsonFormat4(Power)

  // int
  implicit val penaltyFormat = jsonFormat5(Penalty)
  implicit val roundTimeFormat = jsonFormat4(RoundTime)
  implicit val sensorFormat = jsonFormat6(Sensor)
  implicit val velocityFormat = jsonFormat4(Velocity)

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

  val rabbit = Connection()

  // create org.reactivestreams.Subscriber
  val exchange = rabbit.publish(exchange = "accounting_department", routingKey = "invoices")

  def deserialize(delivery: Delivery): JsValue =
    ByteString.fromArray(delivery.message.body.toArray).utf8String.parseJson

  //def deserialize(delivery: Delivery): JsValue =
  //  ByteString.fromArray(delivery.message.body.toArray).utf8String.parseJson

  val sensors = Source.fromPublisher(rabbit.consume(SENSOR_TEMPLATE)).map(deserialize(_).convertTo[Sensor])
  val penalties = Source.fromPublisher(rabbit.consume(PENALTY_TEMPLATE)).map(deserialize(_).convertTo[Penalty])
  val roundTimes = Source.fromPublisher(rabbit.consume(ROUND_PASSED_TEMPLATE)).map(deserialize(_).convertTo[RoundTime])
  val velocities = Source.fromPublisher(rabbit.consume(VELOCITY_TEMPLATE)).map(deserialize(_).convertTo[Velocity])

  val receiver = system.actorOf(Receiver.props)

  Source
    .combine(sensors, penalties, roundTimes, velocities)(Merge(_))
    .runWith(Sink.actorSubscriber(Receiver.props))

  //Sink.fromSubscriber(exchange))

}
