package ff

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import com.typesafe.scalalogging.slf4j.LazyLogging
import ff.messages._
import io.scalac.amqp
import io.scalac.amqp.{Connection, Delivery}
import spray.json.DefaultJsonProtocol._
import spray.json._

object Main extends App with LazyLogging {

  implicit val system = ActorSystem("ff")
  implicit val materializer = ActorMaterializer()

  // out
  implicit val powerFormat = jsonFormat3(Power)

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

  def deserialize(delivery: Delivery): JsValue =
    ByteString.fromArray(delivery.message.body.toArray).utf8String.parseJson

  def serialize(pow: Power): amqp.Message =
    amqp.Message(ByteString(pow.toJson.toString()).toIndexedSeq)

  val sensors = Source.fromPublisher(rabbit.consume(SENSOR_TEMPLATE)).map(deserialize(_).convertTo[Sensor])
  val penalties = Source.fromPublisher(rabbit.consume(PENALTY_TEMPLATE)).map(deserialize(_).convertTo[Penalty])
  val roundTimes = Source.fromPublisher(rabbit.consume(ROUND_PASSED_TEMPLATE)).map(deserialize(_).convertTo[RoundTime])
  val velocities = Source.fromPublisher(rabbit.consume(VELOCITY_TEMPLATE)).map(deserialize(_).convertTo[Velocity])

  val powers = rabbit.publish(exchange = "", routingKey = POWER)

  val publisher =
    Source
      .actorRef(100, OverflowStrategy.dropHead)
      .map { x =>
        println(x)
        x
      }
      .map(serialize)
      .toMat(Sink.fromSubscriber(powers))(Keep.left)
      .run()

  def emitPower(power: Int): Unit =
    publisher ! Power(id, accessCode, power)

  val receiver = system.actorOf(Receiver.props(emitPower))
  val _ =
    Source
      .combine(sensors, penalties, roundTimes, velocities)(Merge(_))
      .runWith(Sink.actorRef(receiver, () => println("finished")))


}
