package tesslasimulator.parser

import tesslasimulator.shared.UnitSpec

class IdentifierParserSpec extends UnitSpec {
  describe("When parsing identifiers") {
    def parse(input: String) = new IdentifierParser(input).Identifier.run()

    it("should accept a single word") {
      assert(parse("identifier").get == "identifier")
    }
    it("should refuse identifiers starting with a digit") {
      assert(parse("2ndIdentifier").isFailure)
    }
    it("should allow identifiers containing underscores") {
      assert(parse("_leadingUnderscore").isSuccess)
      assert(parse("trailingUnderscore_").isSuccess)
      assert(parse("in_between_underscore").isSuccess)
    }
    it("should not allow reserved keywords") {
      val keywords = Seq("define", "Signal", "Event", "if", "else", "Bool", "Int", "Float", "String", "Void", "t")
      for (keyword <- keywords) assert(parse(keyword).isFailure, s"on '$keyword'")
    }
    it("should allow reserved keywords as prefixes") {
      assert(parse("definedThreshold").get == "definedThreshold")
    }
    it("should not allow literals") {
      val literals = Seq("1", "1.0", "true", "false", """"string"""", "#")
      for (literal <- literals) assert(parse(literal).isFailure, s"on '$literal'")
    }
  }
}
