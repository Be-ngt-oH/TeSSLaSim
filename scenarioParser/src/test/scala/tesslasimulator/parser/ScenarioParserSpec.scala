package tesslasimulator.parser

import org.parboiled2.ParseError
import tesslasimulator.shared._
import tesslasimulator.parser.ScenarioAst._
import tesslasimulator.parser.ScenarioAst.LiteralNode.Conversions._

import scala.util.Failure

class ScenarioParserSpec extends UnitSpec {
  /** Short alias for creating a ScenarioParser instance with an input */
  def parser(input: String): ScenarioParser = new ScenarioParser(input)

  describe("When parsing types") {
    describe("for signals") {
      def parse(input: String) = parser(input).SignalType.run()

      it("should recognize all basic types") {
        assert(parse("<Boolean>").get == BoolType)
        assert(parse("<Int>").get == IntType)
        assert(parse("<Float>").get == FloatType)
        assert(parse("<String>").get == StringType)
      }
      it("should fail on unknown types") {
        assert(parse("<UnknownType>").isFailure)
      }
      it("should fail on empty string") {
        assert(parse("<>").isFailure)
      }
      it("should allow leading whitespace") {
        assert(parse("<   Int>").get == IntType)
      }
      it("should allow trailing whitespace") {
        assert(parse("<Int    >").get == IntType)
      }
    }
    describe("for events") {
      def parse(input: String) = parser(input).EventType.run()

      it("should recognize all basic types and void") {
        assert(parse("<Boolean>").get == BoolType)
        assert(parse("<Int>").get == IntType)
        assert(parse("<Float>").get == FloatType)
        assert(parse("<String>").get == StringType)
        assert(parse("<Unit>").get == NoValueType)
      }
      it("should fail on unknown types") {
        assert(parse("<UnknownType>").isFailure)
      }
      it("should fail on empty string") {
        assert(parse("<>").isFailure)
      }
      it("should allow leading whitespace") {
        assert(parse("<   Unit>").get == NoValueType)
      }
      it("should allow trailing whitespace") {
        assert(parse("<Unit    >").get == NoValueType)
      }
    }
  }

  describe("When parsing definitions") {
    describe("When parsing constant definitions") {
      def parse(input: String) = parser(input).ConstDefinition.run()

      val PiDefinition = ConstDefinitionNode("PI", 3.14)

      it("should create a ConstDefinitionNode") {
        assert(parse("define PI = 3.14").get == PiDefinition)
      }
      it("should not allow generator expressions") {
        assert(parse("define PI = t").isFailure)
      }
      describe("when dealing with whitespace") {
        it("should require at least one whitespace character after define") {
          assert(parse("definePI=3.14").isFailure)
        }
        it("should allow whitespace after define") {
          assert(parse("define   PI=3.14").get == PiDefinition)
        }
        it("should allow optional whitespace after the identifier") {
          assert(parse("define PI     =3.14").get == PiDefinition)
        }
        it("should allow whitespace after =") {
          assert(parse("define PI=   3.14").get == PiDefinition)
        }
      }
    }

    describe("When parsing stream definitions") {
      def parse(input: String) = parser(input).StreamDefinition.run()

      val doorOpenNode = EventDefinitionNode(BoolType, "doorOpen")
      val temperatureNode = SignalDefinitionNode(IntType, "temperature", 10)

      describe("when dealing with event streams") {
        it("should create an EventDefinitionNode") {
          assert(parse("Events<Boolean>doorOpen").get == doorOpenNode)
        }
        it("should allow whitespace after the type annotation") {
          assert(parse("Events<Boolean>    doorOpen").get == doorOpenNode)
        }
      }

      describe("when dealing with signals") {
        it("should create a SignalDefinitionNode") {
          assert(parse("Signal<Int>temperature=10").get == temperatureNode)
        }
        it("should require an initial value") {
          assert(parse("Signal<Int>temperature").isFailure)
        }
        it("should not allow generator expressions as initial value") {
          assert(parse("Signal<Int>temperature=t").isFailure)
        }
        it("should allow constant references as initial value") {
          val freezingDef = ConstDefinitionNode("FREEZING", -1)
          def parse(input: String) = new ScenarioParser(
            input, Map("FREEZING" -> freezingDef)
          ).StreamDefinition.run()
          assert(parse("Signal<Int>temperature=FREEZING").get == SignalDefinitionNode(
            IntType, "temperature", ConstantReferenceNode(freezingDef)
          ))
        }
        describe("when dealing with whitespace") {
          it("should allow whitespace after the type annotation") {
            assert(parse("Signal<Int>   temperature=10").get == temperatureNode)
          }
          it("should allow whitespace after the name") {
            assert(parse("Signal<Int>temperature    =10").get == temperatureNode)
          }
          it("should allow whitespace after =") {
            assert(parse("Signal<Int>temperature=    10").get == temperatureNode)
          }
        }
      }
    }
  }

  describe("When parsing timestamps") {
    describe("when parsing for events") {
      def parse(input: String) = parser(input).EventTimestamps.run()

      it("should recognize sole timestamps") {
        assert(parse("42").get == Seq(SingleTimeNode(42)))
      }
      it("should recognize time series") {
        assert(parse("1,..,42").get == Seq(TimeSeriesNode(1, 42)))
      }
      it("should recognize multiple timestamps") {
        assert(parse("1,3,..,5").get == Seq(SingleTimeNode(1), TimeSeriesNode(3, 5)))
      }
      it("should allow whitespace everywhere") {
        assert(parse("1  ,  3  ,  ..  ,  5").get == Seq(SingleTimeNode(1), TimeSeriesNode(3, 5)))
      }
      it("should fail on negative time series") {
        assert(parse("10,..,5").isFailure)
      }
    }
    describe("when parsing for signals") {
      def parse(input: String) = parser(input).SignalTimestamps.run()

      it("should recognize sole timestamps") {
        assert(parse("42").get == Seq(SingleTimeNode(42)))
      }
      it("should recognize time spans") {
        assert(parse("1..42").get == Seq(TimeSpanNode(1, 42)))
      }
      it("should recognize multiple timestamps") {
        assert(parse("1,3..5").get == Seq(SingleTimeNode(1), TimeSpanNode(3, 5)))
      }
      it("should allow whitespace everywhere") {
        assert(parse("1  ,  3  ..  5").get == Seq(SingleTimeNode(1), TimeSpanNode(3, 5)))
      }
      it("should fail on negative time spans") {
        assert(parse("10..5").isFailure)
      }
    }
  }

  describe("When parsing assignments") {
    val aDef = SignalDefinitionNode(IntType, "a", 0)
    val bDef = EventDefinitionNode(BoolType, "b")
    val presetDefinitions = Map(
      "a" -> aDef,
      "b" -> bDef
    )
    def parse(input: String) = new ScenarioParser(input, presetDefinitions).Assignment.run()

    def infoParse(input: String) = {
      val run = new ScenarioParser(input, presetDefinitions).Assignment.run()
      run match {
        case Failure(e: ParseError) => {
          info(e.format(input))
          run
        }
        case _ => run
      }
    }

    val simpleAssignment = AssignmentNode(aDef, Seq(SingleTimeNode(1)), 2)

    it("should create an AssignmentNode") {
      assert(infoParse("a(1)=2").get == simpleAssignment)
    }
    it("should allow generator expressions") {
      assert(parse("a(1)=2*t").get == AssignmentNode(
        aDef, Seq(SingleTimeNode(1)), BinaryOperatorNode(2, Multiplication, TNode)
      ))
    }
    describe("when dealing with whitespace") {
      it("should allow whitespace after identifier") {
        assert(parse("a (1)=2").get == simpleAssignment)
      }
      it("should allow whitespace after opening parentheses") {
        assert(parse("a(   1)=2").get == simpleAssignment)
      }
      it("should allow whitespace after closing parentheses") {
        assert(parse("a(1)  =2").get == simpleAssignment)
      }
      it("should allow whitespace after assignment operator") {
        assert(parse("a(1)=  2").get == simpleAssignment)
      }
    }
    it("should fail on undefined references") {
      assert(parse("unknown(1)=2").isFailure)
    }
    it("should fail on using event timestamp notation on signals and vice versa") {
      assert(parse("a(1, .. 2)").isFailure)
      assert(parse("b(1 .. 2)").isFailure)
    }
  }

  describe("When parsing complete scenarios") {
    def parse(input: String) = {
      val run = parser(input).Scenario.run()
      run match {
        case Failure(e: ParseError) => {
          info(e.format(input))
          run
        }
        case _ => run
      }
    }

    val constantDefinition = ConstDefinitionNode("PI", 3.14)
    val signalDefinition = SignalDefinitionNode(IntType, "a", 0)
    val eventDefinition = EventDefinitionNode(BoolType, "b")
    val simpleAssignment = AssignmentNode(signalDefinition, Seq(SingleTimeNode(10)), 42)
    val complexAssignment = AssignmentNode(
      eventDefinition, Seq(TimeSeriesNode(0, 10)), BinaryOperatorNode(BinaryOperatorNode(TNode, Modulo, 2), Equals, 0)
    )

    it("should create a ScenarioNode") {
      assert(parse("define PI = 3.14;Signal<Int> a = 0;").get == ScenarioNode(
        Seq(constantDefinition, signalDefinition)
      ))
    }
    it("should allow whitespace at the beginning") {
      assert(parse("    define PI = 3.14;").get == ScenarioNode(Seq(constantDefinition)))
    }
    it("should allow whitespace after statement separator") {
      assert(parse("define PI = 3.14;   Signal<Int> a = 0;").get == ScenarioNode(
        Seq(constantDefinition, signalDefinition)
      ))
    }
    it("should fail on re-definitions") {
      def parse(input: String) = parser(input).Scenario.run()

      assert(parse("define AAA=1; define AAA=2;").isFailure)
      assert(parse("define AAA=1; Signal<Int> AAA=2;").isFailure)
      assert(parse("Signal<Int> AAA=1; define AAA=2;").isFailure)
      assert(parse("Signal<Int> AAA=1; Events<Int> AAA;").isFailure)
      assert(parse("Events<Int> AAA; Events<Int> AAA;").isFailure)
    }
    it("should parse complex scenarios") {
      assert(parse(
        """
           define PI = 3.14;
           Signal<Int> a = 0;
           Events<Boolean> b;

           a(10) = 42;
           b(0, .., 10) = t % 2 == 0;
        """
      ).get == ScenarioNode(
        Seq(constantDefinition, signalDefinition, eventDefinition, simpleAssignment, complexAssignment)
      ))
    }
  }

  describe("Method parseScenario") {
    import ScenarioParser.parseScenario

    it("should work") {
      parseScenario(
        """
           Signal<Int> a = 0;

           a(10, 15 .. 20, 25) = t % 2;
        """
      )
    }
  }
}
