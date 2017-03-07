package tesslasimulator.shared

import io.circe._
import io.circe.syntax._

/**
 * Provides implicit [[Encoder]]s for our domain specific objects.
 *
 * Note for IntelliJ-users (29-12-2016): Some expressions are reported to be erroneous ("Expression of type String
 * doesn't conform to expected type Nothing"). However these are correct and compile just fine!
 */
object StreamJsonCodec {
  implicit val encodeValue: Encoder[Any] = new Encoder[Any] {
    final def apply(a: Any): Json = a match {
      case v: StreamBool => v.asJson
      case v: StreamInt => v.asJson
      case v: StreamFloat => v.asJson
      case v: StreamString => v.asJson
      case NoValueEvent => '#'.asJson
    }
  }

  implicit val encodeValueType: Encoder[ValueType] = Encoder.encodeString.contramap(_.toString)
  implicit val encodeTimestampValue: Encoder[(Int, Any)] =
    Encoder.forProduct2("t", "v")(tv => tv)
  implicit val encodeEventStream: Encoder[EventStream] =
    Encoder.forProduct4(
      "name", "type", "valueType", "values"
    )(s => (s.name, "EventStream", s.valueType, s.values))
  implicit val encodeSignalStream: Encoder[SignalStream] =
    Encoder.forProduct5(
      "name", "type", "valueType", "initialValue", "values"
    )(s => (s.name, "SignalStream", s.valueType, s.initialValue, s.values))

  implicit val encodeStream: Encoder[Stream] = new Encoder[Stream] {
    final def apply(s: Stream): Json = s match {
      case s: SignalStream => encodeSignalStream.apply(s)
      case s: EventStream => encodeEventStream.apply(s)
    }
  }
}

object ErrorJsonCodec {
  import tesslasimulator.shared.Error.Error

  implicit val encodeError: Encoder[Error] =
    Encoder.forProduct2("type", "message")(e => (e.getClass.getSimpleName, e.format))
}
