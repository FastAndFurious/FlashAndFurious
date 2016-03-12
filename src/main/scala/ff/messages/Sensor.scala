package ff.messages

case class Sensor(
                   raceTrackId: String,
                   timeStamp: Int,
                   a: (Int, Int, Int), // acceleration x, y, z
                   g: (Int, Int, Int), // gyro x, y, z
                   m: (Int, Int, Int), // mag x, y, z
                   t: Int
                 ) extends Message
