package tesslasimulator.shared

/**
 * Representation of a stream which basically is a name tied to a sequence of events. This interface works for both
 * signals and event streams because signals can be represented as a sequence of value-change events.
 */
sealed trait Stream {
  val name: String
  val valueType: ValueType

  /**
   * Returns a sequence of value changes.
   *
   * @return sequence of timestamp-value tuples
   */
  val values: Seq[(Int, Any)]

  require(
    values.sliding(2).forall {
      case Seq((t1, _), (t2, _)) => t1 < t2
      case _ => true
    },
    s"Values are not sorted by timestamps or timestamps are repeated: ${values}"
  )

  values.foreach {
    case (_, v) =>
      val typeFromAny = valueTypeFromAny(v)
      require(
        typeFromAny == valueType || (valueType == FloatType && typeFromAny == IntType),
        s"Found value of type ${typeFromAny}, but expected $valueType"
      )
  }
}

/**
 * A stream of type signal which holds a list of timestamp-value tuples marking changes of its value and an initial
 * value.
 *
 * @param name         the name of the defined signal stream
 * @param valueType    the stream type as one of [[ValueType]]
 * @param initialValue the initial value which the stream has before it's first change
 * @param values       a list of value changes
 */
case class SignalStream(name: String, valueType: ValueType, initialValue: Any, values: Seq[(Int, Any)])
  extends Stream {
  require(valueType != NoValueType, "SignalStream always has values per definition and thus cannot be of NoValueType")

  require(
    valueTypeFromAny(initialValue) == valueType
      || (valueType == FloatType && valueTypeFromAny(initialValue) == IntType),
    s"Type of initialValue does not match valueType: ${initialValue.getClass} != ${valueType}"
  )

  require(
    values.sliding(2).forall {
      case Seq((_, v1), (_, v2)) => v1 != v2
      case _ => true
    },
    s"Contains obsolete values: ${values}"
  )
}

object ConstantStream {
  def apply(valueType: ValueType, value: Any): SignalStream = SignalStream(value.toString, valueType, value, Seq())
}

/**
 * A stream of type event which holds a list of timestamp-value tuples. This directly maps to [[Stream.values]].
 *
 * @param name      the name of the defined event stream
 * @param valueType the stream type as one of [[ValueType]]
 * @param values    an event sequence
*/
case class EventStream(name: String, valueType: ValueType, values: Seq[(Int, Any)]) extends Stream {
  /**
   * Returns a new stream containing the values of this stream and the passed new ones. Passed values can replace
   * old values.
   *
   * @param values the new values
   * @return a new stream containing old and new values
   */
  def mergeIn(values: Seq[(Int, Any)]): EventStream = {
    val newValueSeq =
      (this.values ++ values)
        // First of all we sort the values by their timestamp
        .sortBy(_._1)
        // Then we handle overrides by only taking the last assignment to a timestamp.
        // (Note: This works because sortBy is stable)
        // (Second note: This also deals with duplicates)
        .foldRight(List[(Int, Any)]())(
        (stampValue, l) => l match {
          case head :: tail =>
            // Check if we're dealing with two values of the same timestamp
            if (stampValue._1 == head._1) {
              // If it's the case we'll drop the left element
              // (case new value overrides old value)
              l
            } else {
              // Met a new timestamp, which means that all assignments at a timestamp were reduced to one.
              stampValue :: head :: tail
            }
          case Nil => stampValue :: Nil
        })

    EventStream(this.name, valueType, newValueSeq)
  }
}
