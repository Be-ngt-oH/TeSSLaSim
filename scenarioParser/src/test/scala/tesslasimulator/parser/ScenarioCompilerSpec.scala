package tesslasimulator.parser

import org.scalatest.OptionValues._
import org.scalatest.prop.TableDrivenPropertyChecks
import tesslasimulator.shared._
import tesslasimulator.parser.ScenarioAst._
import tesslasimulator.parser.ScenarioAst.LiteralNode.Conversions._
import tesslasimulator.parser.ScenarioCompiler._
import tesslasimulator.shared.{EventStream, SignalStream}

class ScenarioCompilerSpec extends UnitSpec with TableDrivenPropertyChecks {
  val constantDefinition = ConstDefinitionNode("PI", 3.14)
  val constantReference = ConstantReferenceNode(constantDefinition)

  describe("Method compileScenarioDescription") {
    val signalDefinition = SignalDefinitionNode(IntType, "a", 0)
    val eventDefinition = EventDefinitionNode(IntType, "b")

    def compile(statements: StatementNode*) = compileScenarioDescription(
      ScenarioNode(Seq(signalDefinition, eventDefinition) ++ statements)
    )

    describe("when dealing with signals") {
      it("should create stream without changes for signal definitions") {
        val streamValues = compile()
        streamValues should contain key "a"
        streamValues("a") should be(
          SignalStream("a", IntType, 0, Seq())
        )
      }

      it("should include two change events on a single assignment that differs from default value") {
        val streamValues = compile(AssignmentNode(signalDefinition, Seq(SingleTimeNode(10)), 42))
        streamValues should contain key "a"
        streamValues("a").values should be(
          Seq((10, 42), (11, 0))
        )
      }

      it("should create stream without changes if all assignments assign the default value") {
        val streamValues = compile(AssignmentNode(signalDefinition, Seq(SingleTimeNode(10)), 0))
        streamValues should contain key "a"
        streamValues("a") should be(
          SignalStream("a", IntType, 0, Seq())
        )
      }

      it("should include two change events for a simple time span assignment") {
        val streamValues = compile(AssignmentNode(signalDefinition, Seq(TimeSpanNode(10, 15)), 42))
        streamValues should contain key "a"
        streamValues("a").values should be(
          Seq((10, 42), (15, 0))
        )
      }

      it("should work for assignments with multiple sorted timestamps") {
        val streamValues = compile(
          AssignmentNode(signalDefinition, Seq(TimeSpanNode(10, 15), TimeSpanNode(17, 23)), 42)
        )
        streamValues should contain key "a"
        streamValues("a").values should be(
          Seq((10, 42), (15, 0), (17, 42), (23, 0))
        )
      }

      it("should work for assignments with multiple unsorted timestamps") {
        val streamValues = compile(
          AssignmentNode(signalDefinition, Seq(TimeSpanNode(17, 23), TimeSpanNode(10, 15)), 42)
        )
        streamValues should contain key "a"
        streamValues("a").values should be(
          Seq((10, 42), (15, 0), (17, 42), (23, 0))
        )
      }

      it("should work for multiple assignments") {
        val streamValues = compile(
          AssignmentNode(signalDefinition, Seq(TimeSpanNode(10, 15)), 42),
          AssignmentNode(signalDefinition, Seq(TimeSpanNode(7, 13)), 5)
        )
        streamValues should contain key "a"
        streamValues("a").values should be(
          Seq((7, 5), (13, 42), (15, 0))
        )
      }

      it("should not include values of conditionals if conditional is not met") {
        val streamValues = compile(AssignmentNode(signalDefinition, Seq(SingleTimeNode(3)), ConditionalNode(3, false)))
        streamValues should contain key "a"
        streamValues("a") should be(
          SignalStream("a", IntType, 0, Seq())
        )
      }

      it("should work with conditionals where condition is met sometimes") {
        val streamValues = compile(
          // a(10 .. 20) = 1 if t % 2 == 0
          AssignmentNode(signalDefinition, Seq(TimeSpanNode(10, 20)), ConditionalNode(1,
            BinaryOperatorNode(
              BinaryOperatorNode(TNode, Modulo, 2),
              Equals,
              0)))
        )
        streamValues should contain key "a"
        streamValues("a").values should be(
          Seq((10, 1), (11, 0), (12, 1), (13, 0), (14, 1), (15, 0), (16, 1), (17, 0), (18, 1), (19, 0))
        )
      }

      it("should work as intended with two conditionals with inverse conditions") {
        val streamValues = compile(
          // a(10 .. 20) = 1 if t % 2 == 0
          AssignmentNode(signalDefinition, Seq(TimeSpanNode(10, 20)), ConditionalNode(1,
            BinaryOperatorNode(
              BinaryOperatorNode(TNode, Modulo, 2),
              Equals,
              0))),
          // a(10 .. 20) = 2 if t % 2 != 0
          AssignmentNode(signalDefinition, Seq(TimeSpanNode(10, 20)), ConditionalNode(2,
            BinaryOperatorNode(
              BinaryOperatorNode(TNode, Modulo, 2),
              EqualsNot,
              0)))
        )
        streamValues should contain key "a"
        streamValues("a").values should be(
          Seq((10, 1), (11, 2), (12, 1), (13, 2), (14, 1), (15, 2), (16, 1), (17, 2), (18, 1), (19, 2), (20, 0))
        )
      }
    }

    describe("when dealing with event streams") {
      it("should create stream without events") {
        val streamValues = compile()
        streamValues should contain key "b"
        streamValues("b") should be(
          EventStream("b", IntType, Seq())
        )
      }

      it("should include events from a single time assignment") {
        val singleTimeAssignment = AssignmentNode(eventDefinition, Seq(SingleTimeNode(10)), 42)
        val streamValues = compile(singleTimeAssignment)
        streamValues should contain key "b"
        streamValues("b").values should be(
          Seq((10, 42))
        )
      }

      it("should include events from a time series assignment") {
        val timeSeriesAssignment = AssignmentNode(eventDefinition, Seq(TimeSeriesNode(10, 15)), 42)
        val streamValues = compile(timeSeriesAssignment)
        streamValues should contain key "b"
        streamValues("b").values should be(
          Seq((10, 42), (11, 42), (12, 42), (13, 42), (14, 42), (15, 42))
        )
      }

      it("should work with multiple sorted timestamps") {
        val multiAssignment = AssignmentNode(
          eventDefinition, Seq(SingleTimeNode(10), SingleTimeNode(15), SingleTimeNode(23)), 42)
        val streamValues = compile(multiAssignment)
        streamValues should contain key "b"
        streamValues("b").values should be(
          Seq((10, 42), (15, 42), (23, 42))
        )
      }

      it("should work with multiple unsorted timestamps") {
        val multiAssignment = AssignmentNode(
          eventDefinition, Seq(SingleTimeNode(15), SingleTimeNode(10), SingleTimeNode(23), SingleTimeNode(12)), 42)
        val streamValues = compile(multiAssignment)
        streamValues should contain key "b"
        streamValues("b").values should be(
          Seq((10, 42), (12, 42), (15, 42), (23, 42))
        )
      }

      it("should work with multiple assignments ") {
        val assignmentA = AssignmentNode(eventDefinition, Seq(TimeSeriesNode(10, 15)), 42)
        val assignmentB = AssignmentNode(eventDefinition, Seq(TimeSeriesNode(8, 12)), 23)
        val streamValues = compile(assignmentA, assignmentB)
        streamValues should contain key "b"
        streamValues("b").values should be(
          Seq((8, 23), (9, 23), (10, 23), (11, 23), (12, 23), (13, 42), (14, 42), (15, 42))
        )
      }

      it("should work with conditionals") {
        val streamValues = compile(
          // b(10, .., 20) = 1 if t % 2 == 0
          AssignmentNode(eventDefinition, Seq(TimeSeriesNode(10, 20)), ConditionalNode(1,
            BinaryOperatorNode(
              BinaryOperatorNode(TNode, Modulo, 2),
              Equals,
              0))),
          // b(10, .., 20) = 2 if t % 2 != 0
          AssignmentNode(eventDefinition, Seq(TimeSeriesNode(10, 20)), ConditionalNode(2,
            BinaryOperatorNode(
              BinaryOperatorNode(TNode, Modulo, 2),
              EqualsNot,
              0)))
        )
        streamValues should contain key "b"
        streamValues("b").values should be(
          Seq((10, 1), (11, 2), (12, 1), (13, 2), (14, 1), (15, 2), (16, 1), (17, 2), (18, 1), (19, 2), (20, 1))
        )
      }
    }
  }

  describe("Method evaluate") {
    it("should evaluate literals") {
      assert(evaluate(true, 0).value == true)
      assert(evaluate(1, 0).value == 1)
      assert(evaluate(1.0, 0).value == 1.0)
      assert(evaluate("string", 0).value == "string")
      assert(evaluate(NoValueEventNode, 0).value == NoValueEvent)
    }

    it("should return the passed timestamp for TNodes") {
      assert(evaluate(TNode, 42).value == 42)
    }

    it("should evaluate constants") {
      assert(evaluate(constantReference, 0).value == 3.14)
    }

    it("should evaluate simple conditionals") {
      assert(evaluate(ConditionalNode(1, true), 0).value == 1)
      assert(evaluate(ConditionalNode(1, false), 0).isEmpty)
    }

    it("should evaluate conditionals containing TNodes and comparisons") {
      val conditional = ConditionalNode(1, BinaryOperatorNode(TNode, Greater, 10))
      assert(evaluate(conditional, 20).value == 1)
      assert(evaluate(conditional, 0).isEmpty)
    }

    describe("when evaluating unary operators") {
      it("should evaluate unary minus for DslInts and DslFloats") {
        assert(evaluate(UnaryOperatorNode(Minus, 1), 0).value == -1)
        assert(evaluate(UnaryOperatorNode(Minus, 1.0), 0).value == -1.0)
      }
      it("should evaluate Not for DslBools") {
        assert(evaluate(UnaryOperatorNode(Not, false), 0).value == true)
      }
    }

    describe("when evaluating binary operators") {
      it("should evaluate equals and equals not for all types") {
        val basicExpressions = Table[ExpressionNode]("expr", true, 1, 1.0, "string", TNode)

        forAll(basicExpressions) {
          expr => {
            assert(evaluate(BinaryOperatorNode(expr, Equals, expr), 42).value == true)
            assert(evaluate(BinaryOperatorNode(expr, EqualsNot, expr), 42).value == false)
          }
        }
      }

      it("should evaluate arithmetic operations for DslInts") {
        val testMatrix =
          Table(
            ("a", "b", "op", "result"),
            (1, 2, Addition, 3),
            (3, 2, Subtraction, 1),
            (3, 4, Multiplication, 12),
            (4, 2, Division, 2),
            (7, 3, Modulo, 1),
            (2, 3, Power, 8)
          )

        forAll(testMatrix) { (a: StreamInt, b: StreamInt, op: BinaryOperator, result: StreamInt) =>
          assert(evaluate(BinaryOperatorNode(a, op, b), 0).value == result)
        }
      }

      it("should evaluate arithmetic operations for DslFloats") {
        val testMatrix =
          Table(
            ("a", "b", "op", "result"),
            (1.0, 2.0, Addition, 3.0),
            (3.0, 2.0, Subtraction, 1.0),
            (3.0, 4.0, Multiplication, 12.0),
            (4.0, 2.0, Division, 2.0),
            (7.0, 3.0, Modulo, 1.0),
            (2.0, 3.0, Power, 8.0)
          )

        forAll(testMatrix) { (a: StreamFloat, b: StreamFloat, op: BinaryOperator, result: StreamFloat) =>
          assert(evaluate(BinaryOperatorNode(a, op, b), 0).value == result)
        }
      }

      it("should evaluate arithmetic operations where one operand is an DslInt and the other a DslFloat") {
        val testMatrix =
          Table(
            ("a", "b", "op", "result"),
            (1, 1.0, Addition, 2.0),
            (3, 3.0, Subtraction, 0.0),
            (3, 3.0, Multiplication, 9.0),
            (4, 4.0, Division, 1.0),
            (3, 3.0, Modulo, 0.0),
            (2, 2.0, Power, 4.0)
          )

        forAll(testMatrix) { (a: StreamInt, b: StreamFloat, op: BinaryOperator, result: StreamFloat) =>
          assert(evaluate(BinaryOperatorNode(a, op, b), 0).value == result)
          assert(evaluate(BinaryOperatorNode(b, op, a), 0).value == result)
        }
      }

      it("should evaluate comparison operators for DslInts") {
        val testMatrix =
          Table(
            ("a", "b", "op"),
            (3, 2, Greater),
            (3, 3, GreaterEqual),
            (2, 3, Less),
            (3, 3, LessEqual)
          )

        forAll(testMatrix) { (a: StreamInt, b: StreamInt, op: BinaryOperator) =>
          assert(evaluate(BinaryOperatorNode(a, op, b), 0).value == true)
        }
      }

      it("should evaluate comparison operators for DslFloats") {
        val testMatrix =
          Table(
            ("a", "b", "op"),
            (3.0, 2.0, Greater),
            (3.0, 3.0, GreaterEqual),
            (2.0, 3.0, Less),
            (3.0, 3.0, LessEqual)
          )

        forAll(testMatrix) { (a: StreamFloat, b: StreamFloat, op: BinaryOperator) =>
          assert(evaluate(BinaryOperatorNode(a, op, b), 0).value == true)
        }
      }

      it("should evaluate comparison operators where one operand is an DslInt and the other a DslFloat") {
        val testMatrix =
          Table(
            ("a", "b", "op"),
            (3, 2, Greater),
            (3, 3, GreaterEqual),
            (2, 3, Less),
            (3, 3, LessEqual)
          )

        forAll(testMatrix) { (a: StreamInt, b: StreamInt, op: BinaryOperator) =>
          assert(evaluate(BinaryOperatorNode(a.asInstanceOf[StreamFloat], op, b), 0).value == true)
          assert(evaluate(BinaryOperatorNode(a, op, b.asInstanceOf[StreamFloat]), 0).value == true)
        }
      }
    }
  }
}
