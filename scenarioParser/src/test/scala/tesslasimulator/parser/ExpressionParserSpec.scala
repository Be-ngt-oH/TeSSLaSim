package tesslasimulator.parser

import tesslasimulator.parser.ExpressionParser._
import tesslasimulator.parser.ScenarioAst._
import tesslasimulator.parser.ScenarioAst.LiteralNode.Conversions._
import tesslasimulator.shared.UnitSpec

class ExpressionParserSpec extends UnitSpec {
  /** Short alias for creating an ExpressionParser instance with an input */
  def parser(input: String, restrictions: Set[ExpressionRestriction] = Set()): ExpressionParser =
    new ExpressionParser(input, restrictions, Map(
      "PI" -> ConstDefinitionNode("PI", 3.14),
      "CONSTANT" -> ConstDefinitionNode("CONSTANT", true)
    ))

  describe("When parsing literals") {
    describe("when parsing integer literals") {
      def parse(input: String) = parser(input).IntLiteral.run()

      it("should recognize single-digit numbers") {
        assert(parse("1").get == IntNode(1))
      }
      it("should recognize zero") {
        assert(parse("0").get == IntNode(0))
      }
      it("should recognize multi-digit numbers") {
        assert(parse("123").get == IntNode(123))
      }
      it("should not recognize multi-digit numbers with a leading zero") {
        assert(parse("0123").isFailure)
      }
      it("should recognize negative single-digit numbers") {
        assert(parse("-1").get == IntNode(-1))
      }
      it("should recognize negative multi-digit numbers") {
        assert(parse("-123").get == IntNode(-123))
      }
      it("should not allow 'negative zero'") {
        assert(parse("-0").isFailure)
      }
    }

    describe("when parsing float literals") {
      def parse(input: String) = parser(input).FloatLiteral.run()

      it("should recognize 1.0") {
        assert(parse("1.0").get == FloatNode(1.0))
      }
      it("should recognize negatives") {
        assert(parse("-1.0").get == FloatNode(-1.0))
      }
      it("should recognize float values 0 < x < 1") {
        assert(parse("0.3").get == FloatNode(0.3))
      }
    }

    describe("when parsing string literals") {
      def parse(input: String) = parser(input).StringLiteral.run()

      it("should recognize simple strings") {
        assert(parse(""""Test"""").get == StringNode("Test"))
      }
      it("should recognize strings containing escaped double quotes") {
        assert(parse(""""This is a \"Test\"."""").get == StringNode("""This is a "Test"."""))
      }
      it("should recognize escaped characters") {
        assert(parse(""""\b\f\n\r\t"""").get == StringNode("\b\f\n\r\t"))
      }
      it("should recognize unicode escapes") {
        assert(parse(""""\u2605"""").get == StringNode("★"))
      }
    }

    describe("when parsing bool literals") {
      def parse(input: String) = parser(input).BoolLiteral.run()

      it("should recognize 'true'") {
        assert(parse("true").get == BoolNode(true))
      }
      it("should recognize 'false'") {
        assert(parse("false").get == BoolNode(false))
      }
    }

    describe("when parsing numeric literals") {
      it("should capture the complete literal") {
        assert(parser("3.14").NumericLiteral.run().get == FloatNode(3.14))
      }
    }

    it("should recognize the NoValueEventLiteral") {
      assert(parser("#").NoValueEventLiteral.run().get == NoValueEventNode)
    }
  }

  describe("When parsing arithmetic expressions") {
    describe("when parsing factors") {
      def parse(input: String) = parser(input).Factor.run()

      it("should allow single numbers") {
        assert(parse("42").isSuccess)
        assert(parse("-42").isSuccess)
      }

      it("should recognize powers") {
        assert(parse("2^12").get == BinaryOperatorNode(2, Power, 12))
      }
      it("should recognize chained powers right-associatively") {
        assert(parse("2^3^4").get == BinaryOperatorNode(2, Power, BinaryOperatorNode(3, Power, 4)))
      }
    }

    describe("when parsing terms") {
      def parse(input: String) = parser(input).Term.run()

      it("should recognize multiplications") {
        assert(parse("2*4").get == BinaryOperatorNode(2, Multiplication, 4))
      }
      it("should recognize divisions") {
        assert(parse("2/4").get == BinaryOperatorNode(2, Division, 4))
      }
      it("should recognize modulos") {
        assert(parse("4%2").get == BinaryOperatorNode(4, Modulo, 2))
      }
      it("should recognize chained operations left-associatively") {
        assert(parse("2*3*4").get == BinaryOperatorNode(BinaryOperatorNode(2, Multiplication, 3), Multiplication, 4))
      }
    }

    def parse(input: String) = parser(input).ArithmeticExpression.run()

    it("should recognize additions") {
      assert(parse("2+3").get == BinaryOperatorNode(2, Addition, 3))
    }
    it("should recognize subtractions") {
      assert(parse("2-3").get == BinaryOperatorNode(2, Subtraction, 3))
    }
    it("should recognize chained operations left-associatively") {
      assert(parse("2+3+4").get == BinaryOperatorNode(BinaryOperatorNode(2, Addition, 3), Addition, 4))
    }
    it("should follow precedence rules") {
      assert(parse("2+3*4").get == BinaryOperatorNode(2, Addition, BinaryOperatorNode(3, Multiplication, 4)))
    }
    it("should allow mixed types (floats and ints)") {
      assert(parse("1+1.0").get == BinaryOperatorNode(1, Addition, 1.0))
    }
    it("should recognize chained unary minus") {
      assert(parse("--1").get == UnaryOperatorNode(Minus, -1))
    }
    it("should recognize references to constants") {
      assert(parse("PI").get == ConstantReferenceNode(ConstDefinitionNode("PI", 3.14)))
    }
    it("should recognize unary minus in front of references to constants") {
      assert(parse("-PI").get == UnaryOperatorNode(Minus, ConstantReferenceNode(ConstDefinitionNode("PI", 3.14))))
    }

    describe("when parsing expressions with parentheses") {
      it("should recognize them") {
        assert(parse("2+(3+4)").get == BinaryOperatorNode(2, Addition, BinaryOperatorNode(3, Addition, 4)))
      }
      it("should recognize unary minus in front of parentheses") {
        assert(parse("-(1+2)").get == UnaryOperatorNode(Minus, BinaryOperatorNode(1, Addition, 2)))
      }
    }
    describe("when dealing with whitespace") {
      it("should allow whitespace between operators") {
        assert(parse("2 + 3 + 4").get == BinaryOperatorNode(BinaryOperatorNode(2, Addition, 3), Addition, 4))
        assert(parse("2 * 3 * 4").get == BinaryOperatorNode(
          BinaryOperatorNode(2, Multiplication, 3), Multiplication, 4)
        )
        assert(parse("2 ^ 3 ^ 4").get == BinaryOperatorNode(2, Power, BinaryOperatorNode(3, Power, 4)))
      }
      it("should allow whitespace after unary minus") {
        assert(parse("- (1+2)").get == UnaryOperatorNode(Minus, BinaryOperatorNode(1, Addition, 2)))
      }
      it("should allow whitespace after parentheses") {
        assert(parse("( 1+2)").get == BinaryOperatorNode(1, Addition, 2))
        assert(parse("(1+2) +3").get == BinaryOperatorNode(BinaryOperatorNode(1, Addition, 2), Addition, 3))
      }
    }
  }

  describe("When parsing boolean expressions") {
    def parse(input: String) = parser(input).BoolExpression.run()

    it("should recognize bool literals") {
      assert(parse("true").get == BoolNode(true))
    }

    it("should recognize binary boolean operators") {
      assert(parse("true&&false").get == BinaryOperatorNode(true, And, false))
      assert(parse("true||false").get == BinaryOperatorNode(true, Or, false))
    }

    it("should follow precedence rules") {
      assert(parse("true||false&&true").get == BinaryOperatorNode(true, Or, BinaryOperatorNode(false, And, true)))
    }

    it("should allow expressions with parentheses") {
      assert(parse("(true||false)&&true").get == BinaryOperatorNode(BinaryOperatorNode(true, Or, false), And, true))
    }

    it("should recognize unary not in front of literals") {
      assert(parse("!true").get == UnaryOperatorNode(Not, true))
    }

    it("should recognize chained unary not") {
      assert(parse("!!true").get == UnaryOperatorNode(Not, UnaryOperatorNode(Not, true)))
    }

    it("should recognize unary not in front of parentheses") {
      assert(parse("!(true&&false)").get == UnaryOperatorNode(Not, BinaryOperatorNode(true, And, false)))
    }

    it("should recognize references to constants") {
      assert(parse("CONSTANT").get == ConstantReferenceNode(ConstDefinitionNode("CONSTANT", true)))
    }

    it("should recognize unary not in front of constants") {
      assert(parse("!CONSTANT").get == UnaryOperatorNode(
        Not, ConstantReferenceNode(ConstDefinitionNode("CONSTANT", true))
      ))
    }

    describe("when dealing with whitespace") {
      it("should allow whitespace after binary boolean operators") {
        assert(parse("true&&   false").get == BinaryOperatorNode(true, And, false))
      }
      it("should allow whitespace after parentheses") {
        assert(parse("( true||false)").get == BinaryOperatorNode(true, Or, false))
        assert(parse("(true||false)  &&true").get == BinaryOperatorNode(
          BinaryOperatorNode(true, Or, false), And, true)
        )
      }
      it("should allow whitespace after unary not") {
        assert(parse("!   true").get == UnaryOperatorNode(Not, true))
      }
    }

    describe("when parsing comparisons") {
      def parse(input: String) = parser(input).Comparison.run()

      it("should recognize all operators") {
        assert(parse("1==1").get == BinaryOperatorNode(1, Equals, 1))
        assert(parse("1!=1").get == BinaryOperatorNode(1, EqualsNot, 1))
        assert(parse("1>=2").get == BinaryOperatorNode(1, GreaterEqual, 2))
        assert(parse("1<=2").get == BinaryOperatorNode(1, LessEqual, 2))
        assert(parse("1>2").get == BinaryOperatorNode(1, Greater, 2))
        assert(parse("1<2").get == BinaryOperatorNode(1, Less, 2))
      }
      it("should allow whitespace after the comparison operator") {
        assert(parse("1>     0").get == BinaryOperatorNode(1, Greater, 0))
      }
      it("should not allow whitespace within two character operator symbols") {
        assert(parse("1>    =0").isFailure)
      }
      it("should allow full-blown arithmetic expressions") {
        val result = parse("2^(2+3) >= 16*2")
        assert(result.isSuccess)
        result.get match {
          case BinaryOperatorNode(lExpr, op, rExpr) =>
            assert(lExpr.isInstanceOf[BinaryOperatorNode])
            assert(op == GreaterEqual)
            assert(rExpr.isInstanceOf[BinaryOperatorNode])
          case _ => fail("Expected expression of type BinaryOperatorNode")
        }
      }
    }

    it("should recognize multiple comparisons in conjunction") {
      assert(parse("1 < 5 && 10 > 6").get == BinaryOperatorNode(
        BinaryOperatorNode(1, Less, 5), And, BinaryOperatorNode(10, Greater, 6)
      ))
    }
  }

  describe("When parsing generator expressions") {
    def parse(input: String) = parser(input).Expression.run()

    it("should allow t in arithmetic expressions") {
      assert(parse("1+t").get == BinaryOperatorNode(1, Addition, TNode))
    }
    it("should allow t in comparisons") {
      assert(parse("t>3").get == BinaryOperatorNode(TNode, Greater, 3))
    }
    it("should allow unary minus in front of t") {
      assert(parse("-t").get == UnaryOperatorNode(Minus, TNode))
    }
    it("should allow whitespace after t") {
      assert(parse("t  +1").get == BinaryOperatorNode(TNode, Addition, 1))
    }
  }

  describe("When parsing Conditions") {
    def parse(input: String) = parser(input).Conditional.run()

    it("should create a ConditionNode") {
      assert(parse("1 if true").get == ConditionalNode(1, true))
    }
    it("should allow superfluous whitespace") {
      assert(parse("1   if true").get == ConditionalNode(1, true))
      assert(parse("1 if   true").get == ConditionalNode(1, true))
    }
  }

  describe("When parsing generic expressions") {
    def parse(input: String) = parser(input).Expression.run()

    it("should recognize expressions of all types") {
      assert(parse("1").get == IntNode(1))
      assert(parse("1.0").get == FloatNode(1))
      assert(parse("1 + 2").get == BinaryOperatorNode(1, Addition, 2))
      assert(parse("true").get == BoolNode(true))
      assert(parse(""""Hello"""").get == StringNode("Hello"))
      assert(parse("#").get == NoValueEventNode)
      assert(parse("1 < 2").get == BinaryOperatorNode(1, Less, 2))
      assert(parse("true && false").get == BinaryOperatorNode(true, And, false))
      assert(parse("t").get == TNode)
      assert(parse("true if 1 < 2").get == ConditionalNode(true, BinaryOperatorNode(1, Less, 2)))
      assert(parse("# if t < 5").get == ConditionalNode(NoValueEventNode, BinaryOperatorNode(TNode, Less, 5)))
      assert(parse("PI").get == ConstantReferenceNode(ConstDefinitionNode("PI", 3.14)))
    }

    it("should fail on undefined constant references") {
      assert(parse("UNKNOWN").isFailure)
    }

    describe("when restrictions are present") {
      it("should fail on t with NoT restriction") {
        assert(parser("t", Set(NoT)).Expression.run().isFailure)
      }
      it("should not fail on everything") {
        assert(parser("1 > 2", Set(NoT)).Expression.run().isSuccess)
      }
    }
  }
}
