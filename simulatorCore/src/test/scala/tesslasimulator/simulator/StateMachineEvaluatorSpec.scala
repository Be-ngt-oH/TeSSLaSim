package tesslasimulator.simulator

import tesslasimulator.shared._
import tesslasimulator.simulator.StateMachineEvaluator._

class StateMachineEvaluatorSpec extends UnitSpec {
  describe("Proposition") {
    describe("method at") {
      it("should work for constant signals") {
        val constantTrue = ConstantStream(BoolType, true)
        val constantFalse = ConstantStream(BoolType, false)
        for (t <- Range(-100, 100)) {
          assert(Proposition(constantTrue, 1).at(t))
          assert(!Proposition(constantFalse, 2).at(t))
        }
      }
      it("should work with dense signals") {
        val signal = SignalStream("", BoolType, false, Seq((1, true), (2, false), (3, true), (4, false)))
        assert(!Proposition(signal, 1).at(0))
        assert(Proposition(signal, 2).at(1))
        assert(!Proposition(signal, 3).at(2))
        assert(Proposition(signal, 4).at(3))
        assert(!Proposition(signal, 5).at(4))
        assert(!Proposition(signal, 6).at(100))
      }
      it("should work for signals with changes") {
        val signal = SignalStream("", BoolType, true, Seq((5, false), (25, true), (99, false), (100, true)))
        assert(Proposition(signal, 1).at(3))
        assert(!Proposition(signal, 2).at(5))
        assert(Proposition(signal, 3).at(27))
        assert(!Proposition(signal, 4).at(99))
        assert(Proposition(signal, 5).at(103))
      }
    }
    describe("method fromArgs") {
      it("works") {
        val (p1, p2) = (ConstantStream(BoolType, true), ConstantStream(BoolType, false))
        val args: Seq[Any] = Seq(p1, p2)
        val propositions = Proposition.fromArgs(args)
        assert(propositions.length == 2)
        propositions should contain only(
          Proposition(p1, 1),
          Proposition(p2, 2)
          )
      }
    }
  }
}
