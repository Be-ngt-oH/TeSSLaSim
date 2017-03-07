package tesslasimulator.parser

import org.parboiled2._

import tesslasimulator.shared._
import tesslasimulator.parser.Error.ParsingError
import tesslasimulator.parser.ExpressionParser._
import tesslasimulator.parser.ScenarioAst._

import scala.util.{Failure, Success}

/**
 * Parboiled2 parser for scenario specifications. It will contain a [[ScenarioAst]] after a successful run. The
 * resulting AST is syntactically and semantically with the notable exception of expression types.
 *
 * @param input the scenario specification
 * @param presetDefinitions (optional) Map of definitions that are externally known
 */
class ScenarioParser(
    val input: ParserInput,
    val presetDefinitions: Map[String, DefinitionNode] = Map[String, DefinitionNode]())
  extends Parser {
  import ScenarioParser._

  // Attention: Mutable!
  // Map that contains all definitions that are found during parsing.
  // It's used to dispatch rules and check if references are undefined during a run.
  var definitions: Map[String, DefinitionNode] = presetDefinitions
  // Returns a map containing constant definitions
  def getConstantDefinitions: Map[String, ConstDefinitionNode] =
    definitions.filter(_._2.isInstanceOf[ConstDefinitionNode]).asInstanceOf[Map[String, ConstDefinitionNode]]

  // TODO: Use quiet and atomic markers to improve error messages
  def Scenario = rule {
    // We have to make sure to reset the mutable state we've introduced on every re-run.
    // Visit https://github.com/sirthias/parboiled2#unchecked-mutable-state for more information.
    run { definitions = presetDefinitions } ~
      zeroOrMore(Whitespace) ~ zeroOrMore(Statement ~ ws(';')) ~ EOI ~> ScenarioNode
  }

  def Statement: Rule1[StatementNode] = rule { Definition | Assignment }

  def Definition = rule {
    (ConstDefinition | StreamDefinition) ~> (definitionNode => {
      definitions += (definitionNode.identifier -> definitionNode)
      push(definitionNode)
    })
  }

  def ConstDefinition = rule {
    ws("define ") ~ FreshIdentifier ~ ws('=') ~ InitialValueExpression ~> ConstDefinitionNode
  }

  def StreamDefinition = rule { EventDefinition | SignalDefinition }

  def EventDefinition = rule { "Events" ~ EventType ~ FreshIdentifier ~> EventDefinitionNode }
  def SignalDefinition = rule {
    "Signal" ~ SignalType ~ FreshIdentifier ~ ws('=') ~ InitialValueExpression ~> SignalDefinitionNode
  }

  def SignalType = rule {
    ws('<') ~
      atomic(Map(
        "Boolean" -> BoolType,
        "Int" -> IntType,
        "Float" -> FloatType,
        "String" -> StringType
      )) ~ zeroOrMore(Whitespace) ~ ws('>')
  }
  def EventType = rule {
    ws('<') ~
      atomic(Map(
        "Boolean" -> BoolType,
        "Int" -> IntType,
        "Float" -> FloatType,
        "String" -> StringType,
        "Unit" -> NoValueType
      )) ~ zeroOrMore(Whitespace) ~ ws('>')
  }

  def Assignment: Rule1[AssignmentNode] = rule {
    Identifier ~> (id => {
      // We're doing three things here. First we're checking if the used identifier was declared, second if the
      // identifier references a constant and third choose a rule on how the timestamps should be specified depending
      // on the referenced stream type (Event or Signal).

      val streamDefinition = definitions get id match {
        case None => rule { failX(s"declared identifier. Identifier '$id' is unknown.") }
        case Some(definition: ConstDefinitionNode) =>
          rule { failX(s"identifier to a stream. Identifier '$id' references a constant.") }
        case Some(streamDefinition) => streamDefinition
      }

      push(streamDefinition.asInstanceOf[StreamDefinitionNode]) ~ ws('(') ~ (
        (test(streamDefinition.isInstanceOf[EventDefinitionNode]) ~ EventTimestamps) |
        (test(streamDefinition.isInstanceOf[SignalDefinitionNode]) ~ SignalTimestamps)
      ) ~ ws(')')
    }) ~ ws('=') ~ Expression ~> AssignmentNode
  }

  def EventTimestamps = rule { oneOrMore(TimeSeries | SingleTime).separatedBy(ws(',')) }
  def SignalTimestamps = rule { oneOrMore(TimeSpan | SingleTime).separatedBy(ws(',')) }

  def SingleTime = rule { IntValue ~> SingleTimeNode }
  def TimeSeries = rule {
    IntValue ~ ws(',') ~ Dots ~ ws(',') ~ IntValue ~> ((t1: Int, t2: Int) => {
      if (t2 > t1)
        push(TimeSeriesNode(t1, t2))
      else
        rule { failX(s"a positive series. Second value $t2 must be greater than $t1.") }
    })
  }
  def TimeSpan = rule {
    IntValue ~ Dots ~ IntValue ~> ((t1: Int, t2: Int) => {
      if (t2 > t1)
        push(TimeSpanNode(t1, t2))
      else
        rule { failX(s"a positive span. Second value $t2 must be greater than $t1.") }
    })
  }

  def Dots = rule { ws("..") }

  def Identifier = rule { runSubParser(new IdentifierParser(_).Identifier) }

  // Like identifier, but additionally checks if the identifier was already used in a definition
  def FreshIdentifier = rule {
    Identifier ~> (id => {
      if (definitions contains id)
        rule { failX(s"more variety. Identifier '$id' is already in use.") }
      else
        push(id)
    })
  }

  // Expressions that must be reducible to constant values, i.e. not include variables
  def InitialValueExpression = rule {
    runSubParser(new ExpressionParser(_, Set(NoT), getConstantDefinitions).Expression)
  }
  // Expressions that represent a constant integer value
  def IntValue = rule { runSubParser(new ExpressionParser(_).IntValue) }
  // General expression
  def Expression = rule { runSubParser(new ExpressionParser(_, Set(), getConstantDefinitions).Expression) }

  // These functions return rules that match the character/string and trailing whitespace
  def ws(c: Char): Rule0 = rule { c ~ zeroOrMore(Whitespace) }
  def ws(s: String): Rule0 = rule { s ~ zeroOrMore(Whitespace) }

  def Whitespace = rule { quiet(WhitespaceCharacters) }
}

object ScenarioParser {
  val WhitespaceCharacters = CharPredicate(" \n\r\t\f")

  /**
   * Parses a scenario specification and returns the corresponding AST as a [[ScenarioNode]] or a sequence of
   * [[ParsingError]]s.
   *
   * @param input the scenario specification
   * @return [[ScenarioNode]] or a sequence of [[ParsingError]]s
   */
  def parseScenario(input: String): Either[ScenarioNode, Seq[ParsingError]] = {
    val parser = new ScenarioParser(input)

    parser.Scenario.run() match {
      case Success(scenarioNode) => Left(scenarioNode)
      case Failure(e: ParseError) => {
        Right(Seq(ParsingError(e, input)))
      }
      case Failure(e) => throw e
    }
  }
}



