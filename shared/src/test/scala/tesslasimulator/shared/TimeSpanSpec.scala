package tesslasimulator.shared

class TimeSpanSpec extends UnitSpec {
  describe("TimeSpan") {
    describe("Cut method") {
      // In comments:
      // A is represented as |----|
      // B is represented as [    ]
      it("should yield correct results when A and B are disjoint, but adjacent") {
        // [    ]|----|
        TimeSpan(10, 20).cut(TimeSpan(20, 30)) should be(
          Seq(TimeSpan(10, 20))
        )
        // |----|[    ]
        TimeSpan(20, 30).cut(TimeSpan(10, 20)) should be(
          Seq(TimeSpan(20, 30))
        )
      }
      it("should yield correct results when A and B are disjoint, but not adjacent") {
        // [    ]  |----|
        TimeSpan(10, 20).cut(TimeSpan(30, 40)) should be(
          Seq(TimeSpan(10, 20))
        )
        // |----|  [    ]
        TimeSpan(30, 40).cut(TimeSpan(10, 20)) should be(
          Seq(TimeSpan(30, 40))
        )
      }
      it("should yield correct results when B is contained in A") {
        // |--[   ]--|
        TimeSpan(10, 40).cut(TimeSpan(20, 30)) should be(
          Seq(TimeSpan(10, 20), TimeSpan(30, 40))
        )
      }
      it("should yield correct results when A is contained in B") {
        // [  |----|  ]
        TimeSpan(20, 30).cut(TimeSpan(10, 40)) should be(
          Seq()
        )
      }
      it("should yield correct results when A and B are identical") {
        // [|----|]
        TimeSpan(10, 40).cut(TimeSpan(10, 40)) should be(
          Seq()
        )
      }
      it("should yield correct results when B partially overlaps A to the right") {
        // |--[--|  ]
        TimeSpan(10, 30).cut(TimeSpan(20, 40)) should be(
          Seq(TimeSpan(10, 20))
        )
        // |--[--|]
        TimeSpan(10, 30).cut(TimeSpan(20, 30)) should be(
          Seq(TimeSpan(10, 20))
        )
      }
      it("should yield correct results when B partially overlaps A to the left") {
        // [  |--]--|
        TimeSpan(20, 40).cut(TimeSpan(10, 30)) should be(
          Seq(TimeSpan(30, 40))
        )
        // [|--]--|
        TimeSpan(10, 30).cut(TimeSpan(10, 20)) should be(
          Seq(TimeSpan(20, 30))
        )
      }
    }
  }
}

