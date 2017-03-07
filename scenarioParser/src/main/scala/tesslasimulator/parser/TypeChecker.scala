package tesslasimulator.parser

import tesslasimulator.shared._

import tesslasimulator.parser.ScenarioAst._
import tesslasimulator.parser.Error._

import scala.util.{Failure, Success, Try}

object TypeChecker {

  /**
   * Checks a scenario for wrongly typed statements. Type errors are collected and returned. Returns an empty
   * sequence if no type errors are present.
   *
   * The scenario is expected to be well-formed i.e. not contain undefined references, assignments to constants etc.
   *
   * @param scenario   the scenario to check
   * @param detectType (optional) function used to determine types of encountered expressions
   * @return Sequence of [[Error]]s
   */
  def checkTypes(
    scenario: ScenarioNode,
    detectType: (ExpressionNode, Map[ConstDefinitionNode, ValueType]) => Try[ValueType] = detectType
  ): Seq[TypeError] = {
    var constantTypes: Map[ConstDefinitionNode, ValueType] = Map()

    scenario.statements.flatMap({
      case constDefinition: ConstDefinitionNode => detectType(constDefinition.value, constantTypes) match {
        case Success(detectedType) => {
          constantTypes += (constDefinition -> detectedType)
          None
        }
        case Failure(typeError: TypeError) => Some(typeError)
        case Failure(exception) => throw exception
      }
      case signalDefinition: SignalDefinitionNode => detectType(signalDefinition.initialValue, constantTypes) match {
        case Success(detectedType) => {
          val annotatedType = signalDefinition.annotatedType
          if (annotatedType == detectedType)
            None
          else if (annotatedType == FloatType && detectedType == IntType)
            None
          else
            Some(TypeMismatchError(annotatedType, detectedType))
        }
        case Failure(typeError: TypeError) => Some(typeError)
        case Failure(exception) => throw exception
      }
      case eventDefinition: EventDefinitionNode => None
      case assignment: AssignmentNode => detectType(assignment.value, constantTypes) match {
        case Success(detectedType) => {
          val annotatedType = assignment.streamDefinition.annotatedType
          if (annotatedType == detectedType)
            None
          else if (annotatedType == FloatType && detectedType == IntType)
            None
          else
            Some(TypeMismatchError(annotatedType, detectedType))
        }
        case Failure(typeError: TypeError) => Some(typeError)
        case Failure(exception) => throw exception
      }
    })
  }

  /**
   * Determines and returns the [[ValueType]] of the given expression. Fails with a [[Error]] if errors are
   * encountered.
   *
   * @param expression    the expression to inspect
   * @param constantTypes a definition-to-type map that is used to check references to constants
   * @return the [[ValueType]] of the expression or [[Error]]
   */
  def detectType(expression: ExpressionNode, constantTypes: Map[ConstDefinitionNode, ValueType]): Try[ValueType] = {
    Try(
      expression match {
        case BinaryOperatorNode(left, operator, right) => {
          return for {
            leftType <- detectType(left, constantTypes)
            rightType <- detectType(right, constantTypes)
          } yield {
            (leftType, operator, rightType) match {
              case (IntType, _: ArithmeticBinaryOperator, IntType) => IntType
              case (_: NumericType, _: ArithmeticBinaryOperator, _: NumericType) => FloatType
              case (x, _: ArithmeticBinaryOperator, y) => throw TypeMismatchError(NumericTypes, Seq(x, y))

              case (_: NumericType, _: ArithmeticComparisonOperator, _: NumericType) => BoolType
              case (x, _: ArithmeticComparisonOperator, y) => throw TypeMismatchError(NumericTypes, Seq(x, y))

              case (BoolType, _: BooleanBinaryOperator, BoolType) => BoolType
              case (x, _: BooleanBinaryOperator, y) => throw TypeMismatchError(BoolType, Seq(x, y))

              case (_, Equals | EqualsNot, _) =>
                if (leftType == rightType)
                  BoolType
                else
                  throw TypeMismatchError(leftType, rightType)
            }
          }
        }

        case UnaryOperatorNode(operator, right) => {
          return for {
            rightType <- detectType(right, constantTypes)
          } yield {
            (operator, rightType) match {
              case (Minus, t: NumericType) => t
              case (Minus, x) => throw TypeMismatchError(NumericTypes, x)
              case (Not, BoolType) => BoolType
              case (Not, x) => throw TypeMismatchError(BoolType, x)
            }
          }
        }

        case ConditionalNode(ifTrue, condition) => {
          return for {
            ifTrueType <- detectType(ifTrue, constantTypes)
            conditionType <- detectType(condition, constantTypes)
          } yield {
            if (conditionType != BoolType) throw TypeMismatchError(BoolType, conditionType)
            ifTrueType
          }
        }

        case ConstantReferenceNode(definition) => constantTypes get definition match {
          case Some(constantType) => constantType
          case None => throw new AssertionError("AST must not contain references to undefined constants.")
        }

        case node: LiteralNode => node match {
          case _: BoolNode => BoolType
          case _: IntNode => IntType
          case _: FloatNode => FloatType
          case _: StringNode => StringType
          case NoValueEventNode => NoValueType
        }

        case TNode => IntType
      }
    )
  }
}