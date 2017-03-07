package tesslasimulator.parser

import org.parboiled2._
import tesslasimulator.parser.ExpressionParser._
import tesslasimulator.parser.ScenarioAst._

/**
 * Parboiled2 parser for expressions used in scenario specifications. It can optionally be restricted to subsets of
 * expressions.
 *
 * @param input the expression string
 * @param restrictions the (optional) set of restrictions for allowed expressions
 */
class ExpressionParser(
    val input: ParserInput,
    val restrictions: Set[ExpressionRestriction] = Set(),
    val constantDefinitions: Map[String, ConstDefinitionNode] = Map())
  extends Parser
  with StringBuilding {

  import tesslasimulator.parser.ScenarioParser.WhitespaceCharacters
  import CharPredicate.{Digit, Digit19, HexDigit}

  // TODO: Use quiet and atomic markers to improve error messages

  def Expression = rule {
    Conditional | ValueExpression
  }

  def ValueExpression = rule { BoolExpression | ArithmeticExpression | StringLiteral | NoValueEventLiteral }

  def Conditional = rule {
    ValueExpression ~ ws("if") ~ BoolExpression ~> ConditionalNode
  }

  def ArithmeticExpression = rule {
    Term ~ zeroOrMore(
      Map("+" -> Addition, "-" -> Subtraction) ~ zeroOrMore(Whitespace) ~ Term ~> BinaryOperatorNode
    )
  }

  def Term = rule {
    Factor ~ zeroOrMore(
      Map(
        "*" -> Multiplication,
        "/" -> Division,
        "%" -> Modulo
      ) ~ zeroOrMore(Whitespace) ~ Factor ~> BinaryOperatorNode
    )
  }

  def Factor: Rule1[ExpressionNode] = rule {
    ArithAtom ~ (ws('^') ~ push(Power)) ~ Factor ~> BinaryOperatorNode |
      ArithAtom
  }

  def ArithAtom: Rule1[ExpressionNode] = rule {
    NumericLiteral | ArithParens | ConstantReference | TimeVariable |
      (ws('-') ~ push(Minus) ~ ArithAtom ~> UnaryOperatorNode)
  }

  def BoolExpression = rule {
    Conjunction ~ zeroOrMore(ws("||") ~ push(Or) ~ Conjunction ~> BinaryOperatorNode)
  }

  def Conjunction = rule {
    BoolAtom ~ zeroOrMore(ws("&&") ~ push(And) ~ BoolAtom ~> BinaryOperatorNode)
  }

  def BoolAtom: Rule1[ExpressionNode] = rule {
    (ws('!') ~ push(Not) ~ BoolAtom ~> UnaryOperatorNode) |
      BoolParens | Comparison | ConstantReference | BoolLiteral
  }

  // TODO: Use meta-rule for expressions in parentheses
  // https://github.com/sirthias/parboiled2#meta-rules
  def ArithParens = rule { ws('(') ~ ArithmeticExpression ~ ws(')') }
  def BoolParens = rule { ws('(') ~ BoolExpression ~ ws(')') }

  def Comparison = rule {
    ArithmeticExpression ~
    Map(
      "==" -> Equals,
      "!=" -> EqualsNot,
      ">=" -> GreaterEqual,
      "<=" -> LessEqual,
      ">" -> Greater,
      "<" -> Less
    ) ~ zeroOrMore(Whitespace) ~ ArithmeticExpression ~> BinaryOperatorNode
  }

  def NumericLiteral = rule {
    // The way PEG work makes it necessary to have FloatLiteral in front of IntLiteral.
    // This is because a prefix of floating point numbers always matches the integer rule
    FloatLiteral | IntLiteral
  }

  def IntLiteral = rule { IntValue ~> IntNode }
  def BoolLiteral = rule { capture("true" | "false") ~ zeroOrMore(Whitespace) ~> (s => s.toBoolean) ~> BoolNode }
  def FloatLiteral = rule {
    capture(Integer ~ '.' ~ oneOrMore(Digit)) ~ zeroOrMore(Whitespace) ~> (s => s.toDouble) ~> FloatNode
  }

  // Using a separate rule for the string matching of an integer allows the re-use of it
  // in e.g. FloatLiteral rule, Timestamp rule
  def IntValue = rule { capture(Integer) ~ zeroOrMore(Whitespace) ~> (s => s.toInt) }
  def Integer = rule {
    optional('-') ~ Digit19 ~ zeroOrMore(Digit) |
      '0' ~ !Digit // The negative lookahead was only added to simplify testing
  }

  // Dealing with strings including unicode and escapes is adapted from
  // https://witt3rd.com/2015/01/06/parboiled2-and-n-triples/
  val QuoteBackslash = CharPredicate("\"\\")
  val QuoteSlashBackSlash = QuoteBackslash ++ "/"
  def StringLiteral = rule { '"' ~ clearSB ~ Characters ~ ws('"') ~ push(StringNode(sb.toString)) }
  def Characters = rule { zeroOrMore(NormalChar | '\\' ~ EscapedChar) }
  def NormalChar = rule { !QuoteBackslash ~ ANY ~ appendSB()}
  def EscapedChar = rule (
    QuoteSlashBackSlash ~ appendSB()
      | 'b' ~ appendSB('\b')
      | 'f' ~ appendSB('\f')
      | 'n' ~ appendSB('\n')
      | 'r' ~ appendSB('\r')
      | 't' ~ appendSB('\t')
      | Unicode ~> { code => sb.append(code.asInstanceOf[Char]); () }
  )
  def Unicode = rule { 'u' ~ capture(HexDigit ~ HexDigit ~ HexDigit ~ HexDigit) ~> (java.lang.Integer.parseInt(_, 16)) }

  def NoValueEventLiteral = rule { ws('#') ~ push(NoValueEventNode) }

  def TimeVariable = rule {
    test(!restrictions.contains(NoT)) ~
      ws('t') ~ push(TNode)
  }

  def ConstantReference = rule {
    runSubParser(new IdentifierParser(_).Identifier) ~> (id => {
      constantDefinitions get id match {
        case None => rule { failX(s"reference to a constant. '$id' is not defined or not a constant.") }
        case Some(definition) => push(definition)
      }
    }) ~> ConstantReferenceNode
  }

  // These functions return rules that match the character/string and trailing whitespace
  def ws(c: Char): Rule0 = rule { c ~ zeroOrMore(Whitespace) }
  def ws(s: String): Rule0 = rule { s ~ zeroOrMore(Whitespace) }

  def Whitespace = rule { quiet(WhitespaceCharacters) }
}
object ExpressionParser {
  sealed trait ExpressionRestriction
  case object NoT extends ExpressionRestriction
}