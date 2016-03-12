package ff

import java.util.logging.LogManager

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.ByteString
import ff.laps.ConstantLaps
import ff.messages._
import io.scalac.amqp
import io.scalac.amqp.{Connection, Delivery}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object Main extends App {

  LogManager.getLogManager.readConfiguration()

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

  implicit val system = ActorSystem("ff")
  implicit val materializer = ActorMaterializer()
  val rabbit = Connection()

  // out
  implicit val powerFormat = jsonFormat3(Power)
  implicit val pilotLifeSignFormat = jsonFormat2(PilotLifeSign)

  // in
  implicit val penaltyFormat = jsonFormat5(Penalty)
  implicit val roundTimeFormat = jsonFormat4(RoundTime)
  implicit val sensorFormat = jsonFormat6(Sensor)
  implicit val velocityFormat = jsonFormat4(Velocity)

  def deserialize(delivery: Delivery): JsValue =
    ByteString.fromArray(delivery.message.body.toArray).utf8String.parseJson

  def serialize(pow: Power): amqp.Message =
    amqp.Message(ByteString(pow.toJson.toString()).toIndexedSeq)

  //val powers = rabbit.publish(exchange = "", routingKey = POWER)
  val announces = rabbit.publish(exchange = "", routingKey = ANNOUNCE)
  val powers = rabbit.publish(exchange = "", routingKey = POWER)

  val sensors = Source.fromPublisher(rabbit.consume(SENSOR_TEMPLATE)).map(deserialize(_).convertTo[Sensor])
  val penalties = Source.fromPublisher(rabbit.consume(PENALTY_TEMPLATE)).map(deserialize(_).convertTo[Penalty])
  val roundTimes = Source.fromPublisher(rabbit.consume(ROUND_PASSED_TEMPLATE)).map(deserialize(_).convertTo[RoundTime])
  val velocities = Source.fromPublisher(rabbit.consume(VELOCITY_TEMPLATE)).map(deserialize(_).convertTo[Velocity])
  val raceStart = Source.fromPublisher(rabbit.consume(START_TEMPLATE)).map(_ => RaceStart)
  val raceStop = Source.fromPublisher(rabbit.consume(STOP_TEMPLATE)).map(_ => RaceStop)

  val publisher =
    Source
      .actorRef[Power](5, OverflowStrategy.dropHead)
      .map(serialize)
      .toMat(Sink.fromSubscriber(powers))(Keep.left)
      .run()

  def emitPower(power: Int): Unit = {
    publisher ! Power(id, accessCode, power)
  }

  val lapper = ConstantLaps.props(110)

  val receiver = system.actorOf(lapper)
  Source
    .combine(sensors, penalties, roundTimes, velocities, raceStart, raceStop)(Merge(_))
      .map{x =>
        println(x)
        x}
    .runWith(Sink.actorRef(receiver, () => println("finished")))

  val keepAlive =
    Source
      .actorRef[amqp.Message](5, OverflowStrategy.dropHead)
      .toMat(Sink.fromSubscriber(announces))(Keep.left)
      .run()
  val alive = amqp.Message(ByteString(PilotLifeSign(id, accessCode).toJson.toString()).toIndexedSeq)
  system.scheduler.schedule(0 second, 1 second, keepAlive, alive)

}
