package tesslasimulator

package object shared {
  /** Enum for supported types */
  sealed trait ValueType {
    override def toString: StreamString = this match {
      case BoolType => "Boolean"
      case IntType => "Int"
      case FloatType => "Float"
      case StringType => "String"
      case NoValueType => "Unit"
    }
  }
  case object BoolType extends ValueType
  sealed trait NumericType extends ValueType
  case object IntType extends NumericType
  case object FloatType extends NumericType
  case object StringType extends ValueType
  case object NoValueType extends ValueType

  val NumericTypes = Seq(IntType, FloatType)

  /**
   * Type aliases that are used everywhere the actual scala types used in stream values are expected.
   *
   * They should make changes from e.g. Int to BigInt easier if they become necessary.
   */
  type StreamBool = Boolean
  type StreamInt = Int
  type StreamFloat = Double
  type StreamString = String

  sealed trait NoValueEvent
  object NoValueEvent extends NoValueEvent {
    override def toString: StreamString = "#"
  }

  /** Can be used to restrict type parameters to supported types, e.g. def f[T: DslType]( ... ) */
  sealed trait StreamType[T]
  object StreamType {
    implicit object StreamBool$ extends StreamType[StreamBool]
    implicit object StreamInt$ extends StreamType[StreamInt]
    implicit object StreamFloat$ extends StreamType[StreamFloat]
    implicit object StreamString$ extends StreamType[StreamString]
    implicit object StreamNoValue$ extends StreamType[NoValueEvent]
  }

  def valueTypeFromAny(v: Any): ValueType = v match {
    case _: StreamBool => BoolType
    case _: StreamInt => IntType
//    case _: BigInt => IntType // FIXME: This is a quick fix that should be removed
    case _: StreamFloat => FloatType
    case _: StreamString => StringType
    case NoValueEvent => NoValueType
    case _ => ???
  }
}
