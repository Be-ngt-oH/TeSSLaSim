package tesslasimulator.shared

class StreamSpec extends UnitSpec {
  describe("EventStream") {
    describe("method mergeIn") {
      it("should add new values") {
        val values = Seq((0, 1), (1, 2))
        assert(EventStream("b", IntType, Seq()).mergeIn(values).values == values)
      }
      it("should override previous values") {
        val oldValues = Seq((0, 1), (1, 2))
        val newValues = Seq((0, 5), (1, 5))
        assert(EventStream("a", IntType, oldValues).mergeIn(newValues).values == newValues)
      }
      it("should not override all previous values") {
        val oldValues = Seq((0, 1), (1, 2))
        val newValues = Seq((1, 5))
        val result = Seq((0, 1), (1, 5))
        assert(EventStream("a", IntType, oldValues).mergeIn(newValues).values == result)
      }
      it("should remove duplicates") {
        val oldValues = Seq((0, 1), (1, 2))
        val newValues = Seq((1, 2), (1, 2), (1, 2))
        val result = Seq((0, 1), (1, 2))
        assert(EventStream("a", IntType, oldValues).mergeIn(newValues).values == result)
      }
      it("should work with empty sequences") {
        val oldValues = Seq((0, 1), (1, 2))
        assert(EventStream("a", IntType, oldValues).mergeIn(Seq()).values == oldValues)
      }
    }
  }
}
