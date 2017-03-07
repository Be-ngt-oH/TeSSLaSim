package tesslasimulator.simulator

import de.uni_luebeck.isp.tessla.StateMachineFunction
import tesslasimulator.shared._
import tesslasimulator.simulator.Error.UnexpectedArgsException

object StateMachineEvaluator {
  case class Proposition(signalStream: SignalStream, n: Int) {
    require(signalStream.valueType == BoolType, "Proposition can only be built from boolean signals")

    /**
     * Returns whether the signal is true at a given time or not
     * @param t1 the timestamp
     * @return true or false depending on the signal's state
     */
    def at(t1: Int): Boolean = {
      // TODO: Optimise this
      var currentValue = signalStream.initialValue.asInstanceOf[Boolean]
      for ((t2, v: Boolean) <- signalStream.values) {
        if (t1 < t2) {
          return currentValue
        }
        currentValue = v
      }

      currentValue
    }
  }
  object Proposition {
    def fromArgs(args: Seq[Any]): Seq[Proposition] = {
      args zip Range(1, args.length + 1) map {
        case (s: SignalStream, n) if s.valueType == BoolType => Proposition(s, n)
        case _ => throw UnexpectedArgsException(args)
      }
    }
  }

  /**
   * Evaluates a state machine and returns an [[EventStream]] with the state's outputs as its values.
   *
   * @param stateMachineFunction the state machine definition (e.g. start, transitions, output)
   * @param args the arguments - Currently constrained to be [[BoolType]]-[[SignalStream]]s and a clock
   * @return a [[StringType]]-[[EventStream]] with the monitor output for each clock step
   */
  def evaluateStateMachine(stateMachineFunction: StateMachineFunction, args: Seq[Any]): Stream = {
    val ticks = args.last match {
      case EventStream(_, NoValueType, values) => values.map(_._1)
      case _ => throw UnexpectedArgsException(args)
    }
    val propositions = Proposition.fromArgs(args.dropRight(1))

    // This map stores all transitions for each state. It's well suited for lookups e.g.
    // transitionMap("q1")(Set(1, 2, 3))
    val transitionMap: Map[String, Map[Set[Int], String]] =
      stateMachineFunction.transitionList.groupBy(_._1).map({
        case (start, transitions) =>
          start -> transitions.map({ case (from, pSet: Set[Int], to: String) => pSet -> to }).toMap
      })

    var currentState = stateMachineFunction.start
    var values = Seq[(Int, String)]()

    for (t <- ticks) {
      val truePropositions = propositions.foldLeft(Set[Int]())((r, p) => {
        if (p.at(t))
          r + p.n
        else
          r
      })
      currentState = transitionMap(currentState)(truePropositions)
      values :+= (t, stateMachineFunction.stateMap(currentState))
    }

    EventStream(stateMachineFunction.name, StringType, values)
  }
}
