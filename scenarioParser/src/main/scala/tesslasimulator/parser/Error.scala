package tesslasimulator.parser

import org.parboiled2.{ParseError, ParserInput}
import tesslasimulator.shared.ValueType
import tesslasimulator.shared.Error._

object Error {
  case class ParsingError(parseError: ParseError, input: ParserInput) extends Error {
    override def format: String = parseError.format(input)
  }

  sealed trait TypeError extends Error
  case class TypeMismatchError(type1: Seq[ValueType], type2: Seq[ValueType])
    extends Exception with TypeError {
    require(type1.nonEmpty, "At least one found type must be provided")
    require(type2.nonEmpty, "At least one expected type must be provided")

    def format: String = {
      val expected =
        if (type1.size < 2)
          type1(0).toString
        else
          s"one of [${type1.mkString(", ")}]"

      val found =
        if (type2.size < 2)
          type2(0).toString
        else
          type2.mkString(" and ")


      s"Expected $expected, but found $found instead."
    }
  }
  object TypeMismatchError {
    def apply(type1: ValueType, type2: ValueType): TypeMismatchError = new TypeMismatchError(Seq(type1), Seq(type2))
    def apply(type1: ValueType, type2: Seq[ValueType]): TypeMismatchError = new TypeMismatchError(Seq(type1), type2)
    def apply(type1: Seq[ValueType], type2: ValueType): TypeMismatchError = new TypeMismatchError(type1, Seq(type2))
  }
}
