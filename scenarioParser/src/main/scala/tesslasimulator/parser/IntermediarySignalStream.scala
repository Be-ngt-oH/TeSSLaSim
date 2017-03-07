package tesslasimulator.parser

import tesslasimulator.shared._

/**
 * A representation of a signal stream to be used while scenario compilation. The values are stored internally as
 * [[TimeSpan]]-value tuples to allow overrides as defined in the DSL (without storing every timestamp-value-tuple).
 *
 * The sequence of [[TimeSpan]]-values always covers the whole range from Int.MinValue to Int.MaxValue without any gaps
 * in it. This is used when converting this internal representation to a proper [[SignalStream]].
 *
 * @param name           the name of the defined signal
 * @param valueType      the stream type as one of [[ValueType]]
 * @param initialValue   the default and initial value of the signal
 * @param timeSpanValues a sequence of intervals with values in which the signal is defined accordingly
 */
case class IntermediarySignalStream(
    name: String,
    valueType: ValueType,
    initialValue: Any,
    timeSpanValues: Seq[(TimeSpan, Any)]) {
  import IntermediarySignalStream._

  // Assertion for the invariant that timeSpanValues always covers the range without gaps
  require(
    timeSpanValues match {
      case zeroOrOneValues if timeSpanValues.length < 2 => true // It's ok

      case _ => timeSpanValues.sliding(2).forall(
        (spans: Seq[(TimeSpan, Any)]) => spans(0).end == spans(1).start
      )
    }, "Time Spans must cover the range seamlessly (t1, t2), (t2, t3), (t3, t4) ...: " +
      timeSpanValues.toString
  )

  lazy val values: Seq[(Int, Any)] = timeSpanValues.map(
    (timeSpanValue: (TimeSpan, Any)) => (timeSpanValue.start, timeSpanValue.value)
  ).dropWhile(
    _._2 == initialValue
  )

  def insert(newTuple: (TimeSpan, Any)): IntermediarySignalStream = {
    // Cut the region out where the new tuple should be added.
    // The result is a sequence based on the current values with exactly one
    // gap for the new value.
    val tuples = timeSpanValues.flatMap(
      tuple => tuple._1.cut(newTuple._1).map(span => (span, tuple.value))
    )

    if (tuples.isEmpty) {
      // Edge case where the new tuple spans the whole range
      return IntermediarySignalStream(name, valueType, initialValue, Seq(newTuple))
    }

    // Insert the new value or unite with the surrounding spans if the values are equal
    val result = tuples.foldRight(Seq[(TimeSpan, Any)]())({
      case (predecessor, successor :: tail) if predecessor.end == newTuple.start =>
        // c is the direct predecessor, head the direct successor of the new tuple
        assert(newTuple.end == successor.start)
        (predecessor.value == newTuple.value, successor.value == newTuple.value) match {
          case (true, true) =>
            // Predecessor, new tuple and successor have same value, unite all
            (TimeSpan(predecessor.start, successor.end), newTuple.value) :: tail
          case (false, true) =>
            // Predecessor is different, unite new tuple and successor
            predecessor :: (TimeSpan(newTuple.start, successor.end), newTuple.value) :: tail
          case (true, false) =>
            // Successor is different, unite predecessor and new tuple
            (TimeSpan(predecessor.start, newTuple.end), newTuple.value) :: successor :: tail
          case (false, false) =>
            // All differ, no unification
            predecessor :: newTuple :: successor :: tail
        }
      case (c, head :: tail) => c :: head :: tail
      case (c, Nil) => c :: Nil
    })

    // Edge cases where newTuple has no predecessor or no successor i.e. it's at the beginning or ending of the range
    if (newTuple.end == tuples.head.start)
      return IntermediarySignalStream(name, valueType, initialValue, newTuple +: tuples)
    if (newTuple.start == tuples.last.end)
      return IntermediarySignalStream(name, valueType, initialValue, tuples :+ newTuple)

    IntermediarySignalStream(name, valueType, initialValue, result)
  }

  def insert(timeSpanValueTuples: Iterable[(TimeSpan, Any)]): IntermediarySignalStream = {
    timeSpanValueTuples.foldLeft(this)((result, current) => result.insert(current))
  }

  def toSignalStream: SignalStream = SignalStream(name, valueType, initialValue, values)
}

object IntermediarySignalStream {
  def apply(name: String, streamType: ValueType, initialValue: Any): IntermediarySignalStream =
    new IntermediarySignalStream(
      name, streamType, initialValue, Seq((TimeSpan(Int.MinValue, Int.MaxValue), initialValue))
    )

  implicit class TimeSpanValue[T](val spanValue: (TimeSpan, T)) {
    val start = spanValue._1.start
    val end = spanValue._1.end
    val value = spanValue._2
  }
}
