package tesslasimulator.parser

import scala.language.implicitConversions

import tesslasimulator.shared._

object ScenarioAst {
  sealed trait AstNode

  case class ScenarioNode(statements: Seq[StatementNode]) extends AstNode

  sealed trait StatementNode extends AstNode

  sealed trait DefinitionNode extends StatementNode {
    val identifier: String
  }
  case class ConstDefinitionNode(identifier: String, value: ExpressionNode) extends DefinitionNode

  sealed trait StreamDefinitionNode extends DefinitionNode {
    val annotatedType: ValueType
    val identifier: String
  }
  case class SignalDefinitionNode(
      annotatedType: ValueType,
      identifier: String,
      initialValue: ExpressionNode)
    extends StreamDefinitionNode
  case class EventDefinitionNode(
      annotatedType: ValueType,
      identifier: String)
    extends StreamDefinitionNode

  case class AssignmentNode(
      streamDefinition: StreamDefinitionNode,
      timestamps: Seq[TimestampNode],
      value: ExpressionNode)
    extends StatementNode

  sealed trait TimestampNode extends AstNode
  case class SingleTimeNode(t: Int) extends TimestampNode
  case class TimeSpanNode(t1: Int, t2: Int) extends TimestampNode
  case class TimeSeriesNode(t1: Int, t2: Int) extends TimestampNode

  sealed trait ExpressionNode extends AstNode

  case class BinaryOperatorNode(
      left: ExpressionNode,
      operator: BinaryOperator,
      right: ExpressionNode)
    extends ExpressionNode

  /** Enum for supported binary operators */
  sealed trait BinaryOperator
  case object Equals extends BinaryOperator
  case object EqualsNot extends BinaryOperator

  sealed trait ArithmeticComparisonOperator extends BinaryOperator
  case object Greater extends ArithmeticComparisonOperator
  case object Less extends ArithmeticComparisonOperator
  case object GreaterEqual extends ArithmeticComparisonOperator
  case object LessEqual extends ArithmeticComparisonOperator

  sealed trait ArithmeticBinaryOperator extends BinaryOperator
  case object Addition extends ArithmeticBinaryOperator
  case object Subtraction extends ArithmeticBinaryOperator
  case object Multiplication extends ArithmeticBinaryOperator
  case object Division extends ArithmeticBinaryOperator
  case object Modulo extends ArithmeticBinaryOperator
  case object Power extends ArithmeticBinaryOperator

  sealed trait BooleanBinaryOperator extends BinaryOperator
  case object And extends BooleanBinaryOperator
  case object Or extends BooleanBinaryOperator

  case class UnaryOperatorNode(
      operator: UnaryOperator,
      right: ExpressionNode)
    extends ExpressionNode

  /** Enum for supported unary operators */
  sealed trait UnaryOperator
  case object Minus extends UnaryOperator
  case object Not extends UnaryOperator

  case class ConditionalNode(ifTrue: ExpressionNode, condition: ExpressionNode) extends ExpressionNode

  sealed trait LiteralNode extends ExpressionNode
  object LiteralNode {
    // These conversions allow using scala values where wrapped values are expected
    // e.g. ConstDefinitionNode("identifier", 42) instead of [...]("identifier"), IntNode(42))
    object Conversions {
      implicit def fromScalaBool(value: Boolean): BoolNode = BoolNode(value)
      implicit def fromScalaInt(value: Int): IntNode = IntNode(value)
      implicit def fromScalaDouble(value: Double): FloatNode = FloatNode(value)
      implicit def fromScalaString(value: String): StringNode = StringNode(value)
    }
  }
  case class BoolNode(value: StreamBool) extends LiteralNode
  case class IntNode(value: StreamInt) extends LiteralNode
  case class FloatNode(value: StreamFloat) extends LiteralNode
  case class StringNode(value: StreamString) extends LiteralNode
  case object NoValueEventNode extends LiteralNode

  case object TNode extends ExpressionNode

  case class ConstantReferenceNode(constDefinition: ConstDefinitionNode) extends ExpressionNode
}
