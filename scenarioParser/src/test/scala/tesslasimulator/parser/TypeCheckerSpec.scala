package tesslasimulator.parser

import scala.util.{Success, Try}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.TryValues._

import tesslasimulator.shared._
import tesslasimulator.parser.ScenarioAst._
import tesslasimulator.parser.ScenarioAst.LiteralNode.Conversions._
import tesslasimulator.parser.Error._

class TypeCheckerSpec extends UnitSpec with TableDrivenPropertyChecks {
  describe("Method detectType") {
    import tesslasimulator.parser.TypeChecker.detectType

    describe("when called with LiteralNode") {
      it("should return correct types for basic literals") {
        assert(detectType(BoolNode(true), Map()) == Success(BoolType))
      }
    }

    describe("when called with TNode") {
      it("should return IntType for it") {
        assert(detectType(TNode, Map()) == Success(IntType))
      }
    }

    describe("when called with UnaryOperatorNode") {
      it("should return IntType for -(1)") {
        assert(detectType(UnaryOperatorNode(Minus, 1), Map()) == Success(IntType))
      }
      it("should return FloatType for -(1.0)") {
        assert(detectType(UnaryOperatorNode(Minus, 1.0), Map()) == Success(FloatType))
      }
      it("should fail with numeric expected on -true") {
        assert(
          detectType(UnaryOperatorNode(Minus, true), Map()).failure.exception
            == TypeMismatchError(NumericTypes, BoolType))
      }
      it("should return BoolType for !true") {
        assert(detectType(UnaryOperatorNode(Not, true), Map()) == Success(BoolType))
      }
      it("should fail with bool expected on !1") {
        assert(detectType(UnaryOperatorNode(Not, 1), Map()).failure.exception
          == TypeMismatchError(BoolType, IntType))
      }
    }

    describe("when called with ConditionalNode") {
      it("should return IntType for 1 if true") {
        assert(detectType(ConditionalNode(1, true), Map()) == Success(IntType))
      }
      it("should fail with bool expected on non-boolean condition") {
        assert(detectType(ConditionalNode(1, 1), Map()).failure.exception
          == TypeMismatchError(BoolType, IntType))
      }
      it("should propagate nested type errors") {
        // !1 if true
        assert(
          detectType(
            ConditionalNode(UnaryOperatorNode(Not, 1), true),
            Map()
          ).failure.exception == TypeMismatchError(BoolType, IntType)
        )
      }
    }

    describe("when called with BinaryOperatorNode") {
      describe("when operator is arithmetic") {
        it("should return IntType if both operands are of IntType") {
          assert(detectType(BinaryOperatorNode(1, Addition, 1), Map()) == Success(IntType))
        }
        it("should return FloatType if one operand is of FloatType") {
          assert(detectType(BinaryOperatorNode(1.0, Addition, 1), Map()) == Success(FloatType))
          assert(detectType(BinaryOperatorNode(1, Addition, 1.0), Map()) == Success(FloatType))
        }
        it("should fail with numeric expected on true + true") {
          assert(
            detectType(
              BinaryOperatorNode(true, Addition, true), Map()
            ).failure.exception == TypeMismatchError(NumericTypes, Seq(BoolType, BoolType)))
        }
        it("should fail with numeric expected on additions of booleans and numbers") {
          assert(
            detectType(
              BinaryOperatorNode(true, Addition, 1),
              Map()
            ).failure.exception == TypeMismatchError(NumericTypes, Seq(BoolType, IntType)))
          assert(
            detectType(
              BinaryOperatorNode(1, Addition, true),
              Map()
            ).failure.exception == TypeMismatchError(NumericTypes, Seq(IntType, BoolType)))
        }
      }

      describe("when operator is boolean") {
        it("should return BoolType if both operands are of BoolType") {
          assert(detectType(BinaryOperatorNode(true, And, true), Map()) == Success(BoolType))
        }
        it("should fail with bool expected if one operand is not of BoolType") {
          val literals = Table[LiteralNode]("literal", 1, 1.0, "string")

          forAll(literals) {
            literal => {
              val failA = detectType(
                BinaryOperatorNode(true, And, literal), Map()
              )
              val failB = detectType(
                BinaryOperatorNode(literal, And, true), Map()
              )

              def assertTypeMismatch(supposedFailure: Try[ValueType]) = {
                assert(supposedFailure.isFailure)
                assert(supposedFailure.failure.exception.isInstanceOf[TypeMismatchError])
                assert(supposedFailure.failure.exception.asInstanceOf[TypeMismatchError].type1 == Seq(BoolType))
              }

              assertTypeMismatch(failA)
              assertTypeMismatch(failB)
            }
          }
        }
      }

      describe("when operator is an equality operator") {
        it("should return BoolType if both sides are of same type") {
          val literals = Table[LiteralNode]("literal", true, 1, 1.0, "string")

          forAll(literals) {
            literal => {
              assert(detectType(BinaryOperatorNode(literal, Equals, literal), Map()) == Success(BoolType))
              assert(detectType(BinaryOperatorNode(literal, EqualsNot, literal), Map()) == Success(BoolType))
            }
          }
        }
        it("should fail with TypeMismatchError on true == 1") {
          assert(
            detectType(
              BinaryOperatorNode(true, Equals, 1),
              Map()
            ).failure.exception == TypeMismatchError(BoolType, IntType)
          )
        }
      }

      describe("when operator is an comparison operator") {
        it("should return BoolType if both sides are of IntType") {
          assert(detectType(BinaryOperatorNode(1, Less, 2), Map()) == Success(BoolType))
        }
        it("should return BoolType if both sides are of FloatType") {
          assert(detectType(BinaryOperatorNode(1.0, Less, 2.0), Map()) == Success(BoolType))
        }
        it("should return BoolType if one side is of IntType and one of FloatType") {
          assert(detectType(BinaryOperatorNode(1, Less, 2.0), Map()) == Success(BoolType))
          assert(detectType(BinaryOperatorNode(1.0, Less, 2), Map()) == Success(BoolType))
        }
        it("should fail with numeric expected if one side is not numeric") {
          val literals = Table[LiteralNode]("literal", true, "string")

          forAll(literals) {
            literal => {
              val failA = detectType(
                BinaryOperatorNode(literal, Less, 1), Map()
              )
              val failB = detectType(
                BinaryOperatorNode(1, Less, literal), Map()
              )

              def assertTypeMismatch(supposedFailure: Try[ValueType]) = {
                assert(supposedFailure.isFailure)
                assert(supposedFailure.failure.exception.isInstanceOf[TypeMismatchError])
                assert(supposedFailure.failure.exception.asInstanceOf[TypeMismatchError].type1 == NumericTypes)
              }

              assertTypeMismatch(failA)
              assertTypeMismatch(failB)
            }
          }
        }
      }
    }

    describe("when called with ConstantReferenceNode") {
      it("should return type stored in map") {
        val piDefinition = ConstDefinitionNode("PI", 3.14)
        assert(detectType(
          ConstantReferenceNode(piDefinition), Map(piDefinition -> FloatType)
        ) == Success(FloatType))
      }
    }

    it("should work with nested ConstantReferenceNodes") {
      val xDefinition = ConstDefinitionNode("x", 1)
      assert(
        detectType(
          BinaryOperatorNode(1, Addition, ConstantReferenceNode(xDefinition)),
          Map(xDefinition -> IntType)
        ) == Success(IntType)
      )
    }
  }

  describe("Method checkTypes") {
    import tesslasimulator.parser.TypeChecker.checkTypes

    val signalDefinition = SignalDefinitionNode(IntType, "a", 0)
    val eventDefinition = EventDefinitionNode(BoolType, "b")
    val constDefinition = ConstDefinitionNode("X", 3)
    val badSignalDefinition = SignalDefinitionNode(IntType, "a", true)

    val timestamps = Seq(SingleTimeNode(1))
    val badSignalAssignment = AssignmentNode(signalDefinition, timestamps, true)
    val badEventAssignment = AssignmentNode(eventDefinition, timestamps, 1)

    val failExpression = BinaryOperatorNode(1, Addition, "fail")
    val testError = TypeMismatchError(NoValueType, NoValueType)

    def detectTypeMock(expr: ExpressionNode, consts: Map[ConstDefinitionNode, ValueType]) = Try(expr match {
      case _: BoolNode => BoolType
      case _: IntNode => IntType
      case _: ConstantReferenceNode => IntType
      case `failExpression` => throw testError
      case _ => ???
    })
    def check = checkTypes(_: ScenarioNode, detectTypeMock)

    describe("when parsing definitions") {
      it("should find no errors on correct definitions") {
        assert(check(ScenarioNode(Seq(signalDefinition))) == Seq())
        assert(check(ScenarioNode(Seq(eventDefinition))) == Seq())
        assert(check(ScenarioNode(Seq(constDefinition))) == Seq())
      }

      it("should fail on badly typed signal definitions") {
        assert(check(
          ScenarioNode(Seq(badSignalDefinition))
        ) == Seq(TypeMismatchError(IntType, BoolType)))
      }

      it("should propagate nested type errors") {
        assert(check(
          ScenarioNode(Seq(SignalDefinitionNode(IntType, "a", failExpression)))
        ) == Seq(testError))
      }
    }

    describe("when parsing assignments") {
      it("should find no errors on correct assignments") {
        val signalAssignment = AssignmentNode(signalDefinition, timestamps, 1)
        val eventAssignment = AssignmentNode(eventDefinition, timestamps, true)

        assert(check(ScenarioNode(Seq(signalDefinition, signalAssignment))) == Seq())
        assert(check(ScenarioNode(Seq(eventDefinition, eventAssignment))) == Seq())
      }

      it("should fail on badly typed assignments") {
        assert(check(
          ScenarioNode(Seq(signalDefinition, badSignalAssignment))
        ) == Seq(TypeMismatchError(IntType, BoolType)))

        assert(check(
          ScenarioNode(Seq(eventDefinition, badEventAssignment))
        ) == Seq(TypeMismatchError(BoolType, IntType)))
      }

      it("should work with assignments referencing constants") {
        val assignment = AssignmentNode(signalDefinition, timestamps, ConstantReferenceNode(constDefinition))

        assert(check(
          ScenarioNode(Seq(constDefinition, signalDefinition, assignment))
        ) == Seq())
      }

      it("should propagate nested type errors on assignments") {
        assert(check(
          ScenarioNode(Seq(signalDefinition, AssignmentNode(signalDefinition, timestamps, failExpression)))
        ) == Seq(testError))
      }
    }

    it("should return one error per badly typed statement") {
      assert(check(
        ScenarioNode(Seq(signalDefinition, eventDefinition, badSignalAssignment, badEventAssignment))
      ) == Seq(TypeMismatchError(IntType, BoolType), TypeMismatchError(BoolType, IntType)))
    }
  }
}
