package ff.filters

class AverageWindow(size: Int, initialValue: Double) extends StatefulFilter {

  assert(size > 2)

  var index = 0
  var sum = size * initialValue
  val values = Array.fill(size)(initialValue)

  override def apply(x: Double): Double = {
    sum = sum + x - values(index)
    values(index) = x
    index = (index + 1) % size
    // remove possible outliers
    sum / size
    //(sum - values.min - values.max) / (size - 2)
  }

}