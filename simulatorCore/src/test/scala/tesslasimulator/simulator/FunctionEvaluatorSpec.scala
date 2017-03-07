package tesslasimulator.simulator

import org.scalatest.Inside
import tesslasimulator.shared._

class FunctionEvaluatorSpec extends UnitSpec with Inside {
  describe("Method evaluateSimpleFunction") {
    import tesslasimulator.simulator.FunctionEvaluator.evaluateSimpleFunction
    import de.uni_luebeck.isp.tessla.TypeVar
    import de.uni_luebeck.isp.tessla.util.SimpleFunctionDSL._

    val a = new TypeVar

    describe("maximum") {
      val fn = Func("maximum").from(Events(a)) × a → Signal(a)
      describe("(Event x D) => Signal") {
        val startInt = ConstantStream(IntType, 10)
        val startFloat = ConstantStream(FloatType, 10.0)
        it("should create empty stream if all events have a smaller value") {
          val eventStream = EventStream("", IntType, Seq((1, 0), (5, 6)))

          inside(evaluateSimpleFunction(fn, Seq(eventStream, startInt))) {
            case SignalStream(_, valueType, initialValue, values) =>
              valueType should be(IntType)
              initialValue should be(10)
              values should be(Seq())
          }
        }
        it("should create empty stream if event stream is empty") {
          val eventStream = EventStream("", IntType, Seq())

          inside(evaluateSimpleFunction(fn, Seq(eventStream, startInt))) {
            case SignalStream(_, valueType, initialValue, values) =>
              valueType should be(IntType)
              initialValue should be(10)
              values should be(Seq())
          }
        }
        it("should create a stream with one change for each event if their values are increasing") {
          val events = Seq((1, 11), (5, 55), (7, 77), (8, 88), (9, 99))
          val eventStream = EventStream("", IntType, events)

          inside(evaluateSimpleFunction(fn, Seq(eventStream, startInt))) {
            case SignalStream(_, valueType, initialValue, values) =>
              valueType should be(IntType)
              initialValue should be(10)
              values should be(events)
          }
        }
        it("should create one change for an alternating event sequence") {
          val eventStream = EventStream("", IntType, Seq((1, 20), (2, 0), (3, 20), (4, 0)))

          inside(evaluateSimpleFunction(fn, Seq(eventStream, startInt))) {
            case SignalStream(_, valueType, initialValue, values) =>
              valueType should be(IntType)
              initialValue should be(10)
              values should be(Seq((1, 20)))
          }
        }
        it("should work with streams of FloatType") {
          val eventStream = EventStream("", FloatType, Seq((1, 20.0)))

          inside(evaluateSimpleFunction(fn, Seq(eventStream, startFloat))) {
            case SignalStream(_, valueType, initialValue, values) =>
              valueType should be(FloatType)
              initialValue should be(10.0)
              values should be(Seq((1, 20.0)))
          }
        }
      }

      describe("Signal => Signal") {
        it("should work as expected") {
          val signalStream = SignalStream("", IntType, 35, Seq((1, 20), (5, 42), (7, 0), (32, 35)))
          inside(evaluateSimpleFunction(fn, Seq(signalStream))) {
            case SignalStream(_, valueType, initialValue, values) =>
              valueType should be(IntType)
              initialValue should be(35)
              values should be(Seq((5, 42)))
          }
        }
      }
    }

    describe("sum") {
      val fn = Func("sum").from(Events(a)) → Signal(a)

      it("should create a constant stream of 0 for empty event stream") {
        val eventStream = EventStream("", IntType, Seq())

        inside(evaluateSimpleFunction(fn, Seq(eventStream))) {
          case SignalStream(_, valueType, initialValue, values) =>
            valueType should be(IntType)
            initialValue should be(0)
            values should be(Seq())
        }
      }

      it("should work as expected with Int-EventStreams") {
        val eventStream = EventStream("", IntType, Seq((1, 10), (5, 20), (6, 3)))

        inside(evaluateSimpleFunction(fn, Seq(eventStream))) {
          case SignalStream(_, valueType, initialValue, values) =>
            valueType should be(IntType)
            initialValue should be(0)
            values should be(Seq((1, 10), (5, 30), (6, 33)))
        }
      }

      it("should work as expected with Float-EventStreams") {
        val eventStream = EventStream("", FloatType, Seq((1, 10.0), (5, 20.0), (6, 3.0)))

        inside(evaluateSimpleFunction(fn, Seq(eventStream))) {
          case SignalStream(_, valueType, initialValue, values) =>
            valueType should be(FloatType)
            initialValue should be(0)
            values should be(Seq((1, 10.0), (5, 30.0), (6, 33.0)))
        }
      }
    }

    describe("abs") {
      describe("Signal => Signal") {
        val fn = Func("abs").from(Signal(a)) → Signal(a)

        it("should squash repeated values") {
          val signalStream = SignalStream("", IntType, 0, Seq((1, -1), (2, 1), (3, -1), (4, 4)))

          inside(evaluateSimpleFunction(fn, Seq(signalStream))) {
            case SignalStream(_, valueType, initialValue, values) =>
              valueType should be(IntType)
              initialValue should be(0)
              values should be(Seq((1, 1), (4, 4)))
          }
        }
      }
      describe("Event => Event") {
        val fn = Func("abs").from(Events(a)) → Events(a)

        it("should work as expected") {
          val eventStream = EventStream("", IntType, Seq((1, -1), (5, 55), (7, -34), (10, -45)))

          inside(evaluateSimpleFunction(fn, Seq(eventStream))) {
            case EventStream(_, valueType, values) =>
              valueType should be(IntType)
              values should be(Seq((1, 1), (5, 55), (7, 34), (10, 45)))
          }
        }
      }
    }

    describe("mrv") {
      val fn = Func("mrv").from(Events(a)) → Signal(a)

      it("should work as expected") {
        val startValue = ConstantStream(StringType, "hello")
        val eventStream = EventStream("", StringType, Seq((1, "a"), (35, "b"), (36, "b"), (42, "c")))

        inside(evaluateSimpleFunction(fn, Seq(eventStream, startValue))) {
          case SignalStream(_, valueType, initialValue, values) =>
            valueType should be(StringType)
            initialValue should be("hello")
            values should be(Seq((1, "a"), (35, "b"), (42, "c")))
        }
      }
    }
  }
}