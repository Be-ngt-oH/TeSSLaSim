package tesslasimulator.parser

import tesslasimulator.parser.ScenarioAst._
import tesslasimulator.shared._

import scala.collection.mutable.ListBuffer

/**
 * Contains functions to transform an AST to a collection of streams and the specified stream values.
 *
 * There are a lot of 'Any's and '???'s floating around in this file. The functions here were written this way
 * because it's quite tricky to pull off dynamic return types etc. I concluded that this (rather blunt) way of
 * doing it is very easy to read, understand and maintain.
 *
 * Please keep in mind that the ASTs we're dealing here with are supposed to be type checked and overall semantically
 * correct (e.g. no reference before use).
 */
object ScenarioCompiler {
  /**
   * Converts a scenario AST to a map of name-stream tuples which contain all value changes of a stream
   * specified in the AST.
   *
   * Please note that the AST is supposed to be semantically correct. Otherwise something _will_ fail (most likely
   * with a [[NotImplementedError]] or a [[ClassCastException]]
   *
   * @param scenario the scenario AST
   * @return identifier-to-stream map that contains all value changes
   */
  def compileScenarioDescription(scenario: ScenarioNode): Map[String, Stream] = {
    var eventStreams: Map[String, EventStream] = Map()
    var signalStreams: Map[String, IntermediarySignalStream] = Map()

    for (statement <- scenario.statements) {
      statement match {
        case SignalDefinitionNode(annotatedType, identifier, initialValueExpression) => {
          val initialValue = evaluate(initialValueExpression, 0).getOrElse(
            throw new AssertionError("Initial value evaluated to None")
          )
          signalStreams +=
            (identifier -> IntermediarySignalStream(identifier, annotatedType, initialValue))
        }
        case EventDefinitionNode(annotatedType, identifier) => {
          eventStreams +=
            (identifier -> EventStream(identifier, annotatedType, Seq()))
        }
        case _: ConstDefinitionNode => // Nothing to be done here

        case AssignmentNode(streamDefinition, timestamps, value) => {
          streamDefinition match {
            case _: EventDefinitionNode => {
              val stream: EventStream = eventStreams.getOrElse(
                streamDefinition.identifier,
                throw new AssertionError("Encountered assignment to unknown stream")
              )

              val events = timestamps.flatMap({
                case SingleTimeNode(t) => Seq(t)
                case TimeSeriesNode(t1, t2) => Range(t1, t2 + 1)
                case _: TimeSpanNode => ???
              }).map(t => {
                (t, evaluate(value, t).getOrElse(None))
              }).filter(
                _._2 != None
              )

              eventStreams += (stream.name -> (stream mergeIn events))
            }

            case _: SignalDefinitionNode => {
              val stream = signalStreams.getOrElse(
                streamDefinition.identifier,
                throw new AssertionError("Encountered assignment to unknown stream")
              )
              val timeSpanValues = timestamps.map({
                case SingleTimeNode(t) => Range(t, t + 1)
                case TimeSpanNode(t1, t2) => Range(t1, t2)
                case _: TimeSeriesNode => ???
              }).flatMap(range => {
                // Imperative programming now so we directly discard duplicate values without putting them in a list.
                // This hopefully prevents us from crashing if users write things like
                // a(Int.MinValue .. Int.MaxValue) = 3
                //
                // Note: It depends on the GC when these are collected.
                val timeSpanValues = ListBuffer[(TimeSpan, Any)]()
                var timeSpanStart = range.start
                var previousValue: Any = None
                // We're going to loop through the range and drop duplicate results. Instead we're storing the
                // intervals in which the same value was assigned.
                for (t <- range) {
                  val newValue: Any = evaluate(value, t).getOrElse(None)
                  if (previousValue != newValue) {
                    (previousValue, newValue) match {
                      case transitionFromNoneToValue if previousValue == None => {
                        timeSpanStart = t
                      }
                      case transitionFromValueToNone if newValue == None => {
                        timeSpanValues += ((TimeSpan(timeSpanStart, t), previousValue))
                      }
                      case transitionFromValueToDifferentValue => {
                        timeSpanValues += ((TimeSpan(timeSpanStart, t), previousValue))
                        timeSpanStart = t
                      }
                    }
                    previousValue = newValue
                  }
                }
                if (previousValue != None) {
                  // Add value of last section (which ends with range.end)
                  timeSpanValues += ((TimeSpan(timeSpanStart, range.end), previousValue))
                }

                timeSpanValues
              })

              signalStreams += (stream.name -> (stream insert timeSpanValues))
            }
          }
        }
      }
    }

    // Join stream maps and replace IntermediarySignalstreams with normal SignalStreams
    (signalStreams.toSeq.map {
      case (name, intermediaryStream) => (name, intermediaryStream.toSignalStream)
    } ++ eventStreams.toSeq).toMap[String, Stream]
  }

  /**
   * Returns the evaluation of an expression at a certain point in time.
   *
   * @param expression the expression to evaluate
   * @param timestamp  the point in time
   * @return the result
   */
  def evaluate(expression: ExpressionNode, timestamp: Int): Option[Any] = {
    expression match {
      case BinaryOperatorNode(left, operator, right) =>
        (evaluate(left, timestamp), evaluate(right, timestamp)) match {
          case (Some(left), Some(right)) =>
            Some(evaluateBinaryOperator(left, operator, right))
          case _ => None
        }

      case UnaryOperatorNode(operator, right) =>
        evaluate(right, timestamp) match {
          case Some(right) =>
            Some(evaluateUnaryOperator(operator, right))
          case _ => None
        }

      case ConditionalNode(ifTrue, condition) =>
        val conditionResult = evaluate(condition, timestamp).getOrElse(false)

        assert(conditionResult.isInstanceOf[Boolean], "Condition evaluation didn't result in a boolean")

        if (conditionResult.asInstanceOf[Boolean])
          evaluate(ifTrue, timestamp)
        else
          None

      case literal: BoolNode => Some(literal.value)
      case literal: IntNode => Some(literal.value)
      case literal: FloatNode => Some(literal.value)
      case literal: StringNode => Some(literal.value)
      case NoValueEventNode => Some(NoValueEvent)

      case TNode => Some(timestamp)

      case ConstantReferenceNode(constDefinition) => evaluate(constDefinition.value, timestamp)
    }
  }

  /**
   * Returns the result of a passed [[BinaryOperator]] applied to the passed operands.
   *
   * Operands and return result are untyped, because the outcome depends on the passed [[BinaryOperator]]. Turns out
   * that it's quite hard to model dynamic return types (e.g. magnet pattern).
   *
   * @param left     the first operand
   * @param operator the [[BinaryOperator]]
   * @param right    the second operand
   * @return the result
   */
  def evaluateBinaryOperator(left: Any, operator: BinaryOperator, right: Any): Any = (left, right) match {
    case (left: StreamInt, right: StreamInt) => operator match {
      case Greater => left > right
      case Less => left < right
      case GreaterEqual => left >= right
      case LessEqual => left <= right

      case Addition => left + right
      case Subtraction => left - right
      case Multiplication => left * right
      case Division => left / right
      case Modulo => left % right
      case Power => math.pow(left, right).asInstanceOf[StreamInt]

      case Equals => left == right
      case EqualsNot => left != right

      case _ => ???
    }

    case (left: StreamFloat, right: StreamFloat) => operator match {
      case Greater => left > right
      case Less => left < right
      case GreaterEqual => left >= right
      case LessEqual => left <= right

      case Addition => left + right
      case Subtraction => left - right
      case Multiplication => left * right
      case Division => left / right
      case Modulo => left % right
      case Power => math.pow(left, right)

      case Equals => left == right
      case EqualsNot => left != right

      case _ => ???
    }

    case (left: StreamInt, right: StreamFloat) => evaluateBinaryOperator(left.asInstanceOf[StreamFloat], operator, right)
    case (left: StreamFloat, right: StreamInt) => evaluateBinaryOperator(left, operator, right.asInstanceOf[StreamFloat])

    case (left: StreamBool, right: StreamBool) => operator match {
      case And => left && right
      case Or => left || right

      case Equals => left == right
      case EqualsNot => left != right

      case _ => ???
    }

    case (left: StreamString, right: StreamString) => operator match {
      case Equals => left == right
      case EqualsNot => left != right

      case _ => ???
    }

    case _ => ???
  }

  /**
   * Returns the result of a passed [[UnaryOperator]] applied to the passed operand.
   *
   * @param operator the [[UnaryOperator]]
   * @param right    the operand
   * @return the result
   */
  def evaluateUnaryOperator(operator: UnaryOperator, right: Any): Any = right match {
    case right: StreamInt => operator match {
      case Minus => -right

      case _ => ???
    }
    case right: StreamFloat => operator match {
      case Minus => -right

      case _ => ???
    }
    case right: StreamBool => operator match {
      case Not => !right

      case _ => ???
    }

    case _ => ???
  }
}


