package tesslasimulator.parser

import tesslasimulator.shared._

class IntermediarySignalStreamSpec extends UnitSpec {
  describe("IntermediarySignalStream") {
    val signal = IntermediarySignalStream("a", IntType, 0)

    def tsv(start: Int, end: Int, value: Any) = (TimeSpan(start, end), value)

    describe("Method insert") {
      it("should work with first insert") {
        signal.insert(tsv(10, 20, 42)).timeSpanValues should contain only(
          tsv(Int.MinValue, 10, 0),
          tsv(10, 20, 42),
          tsv(20, Int.MaxValue, 0)
        )
      }

      it("should work with multiple unrelated inserts") {
        signal.insert(tsv(10, 20, 42)).insert(tsv(30, 40, 55)).timeSpanValues should contain only(
          tsv(Int.MinValue, 10, 0),
          tsv(10, 20, 42),
          tsv(20, 30, 0),
          tsv(30, 40, 55),
          tsv(40, Int.MaxValue, 0)
        )
      }

      it("should not add anything for inserts with same value as the streams initial value") {
        signal.insert(tsv(10, 20, 0)) should be(signal)
      }

      it("should be idempotent") {
        val tuple = tsv(10, 20, 42)
        signal.insert(tuple).insert(tuple) should be(signal.insert(tuple))
      }

      it("should work with Int boundaries") {
        signal.insert(tsv(Int.MinValue, 30, 42)).timeSpanValues should contain only(
          tsv(Int.MinValue, 30, 42),
          tsv(30, Int.MaxValue, 0)
        )
        signal.insert(tsv(30, Int.MaxValue, 42)).timeSpanValues should contain only(
          tsv(Int.MinValue, 30, 0),
          tsv(30, Int.MaxValue, 42)
        )
        signal.insert(tsv(Int.MinValue, Int.MaxValue, 42)).timeSpanValues should contain only
          tsv(Int.MinValue, Int.MaxValue, 42)

        signal.insert(tsv(Int.MinValue, Int.MaxValue, 0)) should be(signal)
      }

      it("should work with two overlapping inserts of different value") {
        signal
          .insert(tsv(10, 20, 42))
          .insert(tsv(15, 25, 5)).timeSpanValues should contain only(
          tsv(Int.MinValue, 10, 0),
          tsv(10, 15, 42),
          tsv(15, 25, 5),
          tsv(25, Int.MaxValue, 0)
        )
      }

      it("should work with two overlapping inserts of same value") {
        val (a, b) = (tsv(10, 20, 42), tsv(15, 25, 42))
        val expected = Seq(tsv(Int.MinValue, 10, 0), tsv(10, 25, 42), tsv(25, Int.MaxValue, 0))
        signal.insert(a).insert(b).timeSpanValues should be(expected)
        signal.insert(b).insert(a).timeSpanValues should be(expected)
      }

      it("should work with complete overrides") {
        signal.insert(tsv(10, 20, 42)).insert(tsv(5, 25, 55)).timeSpanValues should contain only(
          tsv(Int.MinValue, 5, 0),
          tsv(5, 25, 55),
          tsv(25, Int.MaxValue, 0)
        )
        signal.insert(tsv(10, 20, 42)).insert(tsv(5, 55, 0)) should be(signal)
      }

      it("should work as expected with multiple overrides") {
        signal
          .insert(tsv(10, 20, 42))
          .insert(tsv(13, 17, 5))
          .insert(tsv(3, 15, 8)).timeSpanValues should contain only(
          tsv(Int.MinValue, 3, 0),
          tsv(3, 15, 8),
          tsv(15, 17, 5),
          tsv(17, 20, 42),
          tsv(20, Int.MaxValue, 0)
        )
      }
    }

    describe("Method insert with iterable") {
      it("should work as expected") {
        IntermediarySignalStream("a", IntType, 0).insert(
          Seq((TimeSpan(10, 20), 42), (TimeSpan(13, 17), 5), (TimeSpan(3, 15), 8))
        ).timeSpanValues should contain only(
          (TimeSpan(Int.MinValue, 3), 0),
          (TimeSpan(3, 15), 8),
          (TimeSpan(15, 17), 5),
          (TimeSpan(17, 20), 42),
          (TimeSpan(20, Int.MaxValue), 0)
        )
      }
    }

    describe("Method values") {
      // TODO: Add more tests
      it("returns sequence of value changes") {
        IntermediarySignalStream("a", IntType, 0, Seq(
          (TimeSpan(Int.MinValue, 10), 0),
          (TimeSpan(10, 20), 42),
          (TimeSpan(20, Int.MaxValue), 0)
        )).values should be(Seq(
          (10, 42), (20, 0)
        ))
      }

      it("works with boundary values") {
        IntermediarySignalStream("a", IntType, 0, Seq(
          (TimeSpan(Int.MinValue, 10), 42),
          (TimeSpan(10, 20), 0),
          (TimeSpan(20, Int.MaxValue), 95)
        )).values should be(Seq(
          (Int.MinValue, 42), (10, 0), (20, 95)
        ))
      }
    }
  }
}
