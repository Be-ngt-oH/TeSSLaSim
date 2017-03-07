package tesslasimulator.parser

import org.parboiled2._

/**
 * Parboiled2 parser for parsing valid identifiers.
 *
 * @param input the identifier
 */
class IdentifierParser(val input: ParserInput) extends Parser {
  import tesslasimulator.parser.ScenarioParser.WhitespaceCharacters

  def Identifier = rule {
    capture(
      optional(Reserved) ~ (CharPredicate.Alpha | '_') ~ zeroOrMore(CharPredicate.AlphaNum | '_' | '.')
    ) ~ zeroOrMore(Whitespace)
  }

  def Reserved = rule {
    "define" | "Signal" | "Event" | "if" | "else" | "Bool" | "Int" | "Float" |
      "String" | "Void" | "true" | "false" | "t" | "None"
  }

  def Whitespace = rule { quiet(WhitespaceCharacters) }
}
