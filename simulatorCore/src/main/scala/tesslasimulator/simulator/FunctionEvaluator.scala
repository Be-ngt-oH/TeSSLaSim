package tesslasimulator.simulator

import de.uni_luebeck.isp.tessla._
import tesslasimulator.shared._
import tesslasimulator.simulator.Error.{UnexpectedArgsException, UnknownFunctionException}

import scala.collection.SortedSet

object FunctionEvaluator {
  def evaluateFunction(function: Function, args: Seq[Any], inputStreams: Map[String, Stream]): Stream = function match {
    case ConstantValue(_, value) => {
      // We turn literals into constant stream because we know where we expect them. The literal value will be stored
      // as the initial value of the stream, which can easily be extracted.
      // FIXME: Quick fix because TeSSLa uses BigInt, whereas scenario uses BigInt (Should be removed ASAP!)
      value match {
        case v: BigInt => ConstantStream(valueTypeFromAny(v.toInt), v.toInt)
        case v => ConstantStream(valueTypeFromAny(v), v)
      }
    }
    case InputStream(name, _) => inputStreams(name)
    case function: SimpleFunction => evaluateSimpleFunction(function, args)
    case TypeAscription(_) => ??? // Should not happen FIXME: Is this correct?
    case monitor: MonitorFunction => ??? // Should not happen FIXME: Is this correct?
    case stateMachine: StateMachineFunction => StateMachineEvaluator.evaluateStateMachine(stateMachine, args)
  }

  def evaluateSimpleFunction(function: SimpleFunction, args: Seq[Any]): Stream = function.name match {
    case "constantSignal" => args match {
      case (constantStream: SignalStream) :: Nil if constantStream.values.isEmpty => constantStream
      case _ => throw UnexpectedArgsException(args)
    }

    case "add" => combineTwoSignals[StreamInt, StreamInt, StreamInt](function.name, args, _ + _)
    case "add" => combineTwoSignals[StreamFloat, StreamFloat, StreamFloat](function.name, args, _ + _)
    case "sub" => combineTwoSignals[StreamInt, StreamInt, StreamInt](function.name, args, _ - _)
    case "sub" => combineTwoSignals[StreamFloat, StreamFloat, StreamFloat](function.name, args, _ - _)
    case "mul" => combineTwoSignals[StreamInt, StreamInt, StreamInt](function.name, args, _ * _)
    case "mul" => combineTwoSignals[StreamFloat, StreamFloat, StreamFloat](function.name, args, _ * _)
    case "div" => combineTwoSignals[StreamInt, StreamInt, StreamInt](function.name, args, _ / _)
    case "div" => combineTwoSignals[StreamFloat, StreamFloat, StreamFloat](function.name, args, _ / _)

    case "gt" => combineTwoSignals[StreamInt, StreamInt, StreamBool](function.name, args, _ > _)
    case "gt" => combineTwoSignals[StreamFloat, StreamFloat, StreamBool](function.name, args, _ > _)
    case "geq" => combineTwoSignals[StreamInt, StreamInt, StreamBool](function.name, args, _ >= _)
    case "geq" => combineTwoSignals[StreamFloat, StreamFloat, StreamBool](function.name, args, _ >= _)
    case "lt" => combineTwoSignals[StreamInt, StreamInt, StreamBool](function.name, args, _ < _)
    case "lt" => combineTwoSignals[StreamFloat, StreamFloat, StreamBool](function.name, args, _ < _)
    case "leq" => combineTwoSignals[StreamInt, StreamInt, StreamBool](function.name, args, _ <= _)
    case "leq" => combineTwoSignals[StreamFloat, StreamFloat, StreamBool](function.name, args, _ <= _)

    case "eq" => combineTwoSignals[Any, Any, StreamBool](function.name, args, _ == _)

    case "max" => combineTwoSignals(function.name, args, Math.max)
    case "min" => combineTwoSignals(function.name, args, Math.min)

    case "abs" => args match {
      // Signal => Signal
      case SignalStream(_, IntType, _, _) :: Nil =>
        aggregateSignal[StreamInt](function.name, args, (x, _) => Math.abs(x))
      case SignalStream(_, FloatType, _, _) :: Nil =>
        aggregateSignal[StreamFloat](function.name, args, (x, _) => Math.abs(x))

      // Event => Event
      case EventStream(_, valueType: NumericType, values) :: Nil => {
        val results = values.map {
          case (t, v: StreamInt) => (t, Math.abs(v))
          case (t, v: StreamFloat) => (t, Math.abs(v))
          case _ => ???
        }
        EventStream(function.name, valueType, results)
      }

      case _ => throw UnexpectedArgsException(args)
    }

    case "and" => combineTwoSignals[StreamBool, StreamBool, StreamBool](function.name, args, _ && _)
    case "or" => combineTwoSignals[StreamBool, StreamBool, StreamBool](function.name, args, _ || _)
    case "implies" =>
      combineTwoSignals(function.name, args, (a: StreamBool, b: StreamBool) => !a || b)

    case "not" => args match {
      case SignalStream(_, BoolType, initialValue: StreamBool, values) :: Nil =>
        val initial = !initialValue
        val results = values.asInstanceOf[Seq[(Int, StreamBool)]].map({ case (t, v) => (t, !v) })
        SignalStream(function.name, BoolType, initial, results)
      case _ => throw UnexpectedArgsException(args)
    }

    case "neg" => args match {
      case EventStream(_, BoolType, values) :: Nil =>
        val results = values.asInstanceOf[Seq[(Int, StreamBool)]].map({ case (t, v) => (t, !v) })
        EventStream(function.name, BoolType, results)
      case _ => throw UnexpectedArgsException(args)
    }

    case "eventCount" => args match {
      case (EventStream(_, _, values)) :: Nil =>
        val results = values.map(_._1) zip Range(1, values.length + 1)
        SignalStream(function.name, IntType, 0, results)

      case EventStream(_, _, leftValues) :: EventStream(_, _, rightValues) :: Nil => {
        // FIXME: is this correct?
        var (lTs, rTs) = (leftValues.map(_._1), rightValues.map(_._1))
        var count = 0
        var results = Seq[(Int, Int)]()

        while (lTs.nonEmpty || rTs.nonEmpty) {
          (lTs, rTs) match {
            case (t1 :: l, t2 :: r) if t1 < t2 =>
              count += 1
              results :+= (t1, count)
              lTs = l
            case (t1 :: l, t2 :: r) if t1 > t2 =>
              count = 0
              results :+= (t2, 0)
              rTs = r
            case (t1 :: l, t2 :: r) if t1 == t2 =>
              results :+= (t1, count + 1)
              count = 0
              lTs = l
              rTs = r
            case (t1 :: l, Nil) =>
              count += 1
              results :+= (t1, count)
              lTs = l
            case (Nil, t2 :: l) =>
              results :+= (t2, 0)
              rTs = Nil
          }
        }

        SignalStream(function.name, IntType, 0, results)
      }

      case _ => throw UnexpectedArgsException(args)
    }

    case "occursAll" => args match {
      case EventStream(_, _, leftValues) :: EventStream(_, _, rightValues) :: Nil => {
        val results = (leftValues.map(_._1) intersect rightValues.map(_._1)).map(t => (t, NoValueEvent))
        EventStream(function.name, NoValueType, results)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "occursAny" => args match {
      case EventStream(_, _, leftValues) :: EventStream(_, _, rightValues) :: Nil => {
        val results =
          (SortedSet(leftValues.map(_._1): _*) union SortedSet(rightValues.map(_._1): _*)).toSeq.
            map(t => (t, NoValueEvent))

        EventStream(function.name, NoValueType, results)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "merge" => args match {
      case EventStream(_, leftType, leftValues) :: EventStream(_, rightType, rightValues) :: Nil
        if leftType == rightType => {
        // Note: merge function gives precedence to first argument
        EventStream(function.name, leftType, rightValues).mergeIn(leftValues)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "filter" => args match {
      case EventStream(_, leftType, leftValues)
        :: SignalStream(_, BoolType, initialValue: StreamBool, rightValues) :: Nil => {
        var results = Seq[(Int, Any)]()
        var remainingLeft = leftValues
        for ((t, v: StreamBool) <- rightValues) {
          // Note: This looks counter-intuitive at first, but if we encounter a change from false to true it means
          // that the events before the current time stamp must be discarded.
          if (v) {
            remainingLeft = remainingLeft.dropWhile(_._1 < t)
          } else {
            val (a, b) = remainingLeft.span(_._1 < t)
            results ++= a
            remainingLeft = b
          }
        }
        if (initialValue)
          results ++= remainingLeft

        EventStream(function.name, leftType, results)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "ifThen" => args match {
      case EventStream(_, _, leftValues) :: SignalStream(_, rightType, initialValue, rightValues) :: Nil => {
        var results = Seq[(Int, Any)]()
        var currentValue = initialValue
        var remainingLeft = leftValues
        for ((t, v) <- rightValues) {
          val (a, b) = remainingLeft.span(_._1 < t)
          results ++= a.map { case (t, _) => (t, currentValue) }
          currentValue = v
          remainingLeft = b
        }
        results ++= remainingLeft.map { case (t, _) => (t, currentValue) }
        EventStream(function.name, rightType, results)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "ifThenElse" => args match {
      // TODO: Implement this
      case SignalStream(_, BoolType, initialIf, ifValues)
        :: SignalStream(_, thenType, thenInitial, thenValues)
        :: SignalStream(_, elseType, elseInitial, elseValues)
        :: Nil if thenType == elseType => {
        throw UnknownFunctionException(function)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "within" => args match {
        // Int x Int x Events => Signal<Bool>
      case SignalStream(_, IntType, d1: StreamInt, _)
        :: SignalStream(_, IntType, d2: StreamInt, _)
        :: EventStream(_, _, values)
        :: Nil => within(function.name, d1, d2, values)
      case _ => throw UnexpectedArgsException(args)
    }

    case "inPast" => args match {
      case SignalStream(_, IntType, d: StreamInt, _)
        :: EventStream(_, _, values)
        :: Nil => within(function.name, -d, 0, values)
      case _ => throw UnexpectedArgsException(args)
    }

    case "inFuture" => args match {
      case SignalStream(_, IntType, d: StreamInt, _)
        :: EventStream(_, _, values)
        :: Nil => within(function.name, 0, d, values)
      case _ => throw UnexpectedArgsException(args)
    }

    case "mrv" => aggregateEvent[Any](function.name, args, (x, _) => x)

    case "timestamps" => args match {
      case EventStream(_, _, values) :: Nil => {
        val results = values.map({ case (t, _) => (t, t) })
        EventStream(function.name, IntType, results)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "shift" => {
      def shift(values: Seq[(Int, Any)], n: StreamInt = 1) = {
        val (timestamps, v) = values.unzip
        timestamps.drop(n) zip values
      }
      args match {
        case EventStream(_, valueType, values) :: Nil => {
          EventStream(function.name, valueType, shift(values))
        }
        case EventStream(_, valueType, values) :: SignalStream(_, IntType, n: StreamInt, _) :: Nil => {
          EventStream(function.name, valueType, shift(values, n))
        }
        case _ => throw UnexpectedArgsException(args)
      }
    }

    case "changeOf" => args match {
      case SignalStream(_, valueType, _, values) :: Nil => {
        EventStream(function.name, valueType, values)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "on" => args match {
      // FIXME: I'm sure the semantics of this will change
      case EventStream(_, _, values) :: Nil => {
        val results = values.map { case (t, v) => (t, NoValueEvent) }
        EventStream(function.name, NoValueType, results)
      }
      case _ => throw UnexpectedArgsException(args)
    }

    case "maximum" => args match {
      // (Event x D) => Signal
      case EventStream(_, IntType, _) :: SignalStream(_, IntType, _, _) :: Nil =>
        aggregateEvent[StreamInt](function.name, args, Math.max)
      case EventStream(_, FloatType, _) :: SignalStream(_, FloatType, _, _) :: Nil =>
        aggregateEvent[StreamFloat](function.name, args, Math.max)
      // Signal => Signal
      case SignalStream(_, IntType, _, _) :: Nil =>
        aggregateSignal[StreamInt](function.name, args, Math.max)
      case SignalStream(_, FloatType, _, _) :: Nil =>
        aggregateSignal[StreamFloat](function.name, args, Math.max)

      case _ => throw UnexpectedArgsException(args)
    }

    case "minimum" => args match {
      // (Event x D) => Signal
      case EventStream(_, IntType, _) :: SignalStream(_, IntType, _, _) :: Nil =>
        aggregateEvent[StreamInt](function.name, args, Math.min)
      case EventStream(_, FloatType, _) :: SignalStream(_, FloatType, _, _) :: Nil =>
        aggregateEvent[StreamFloat](function.name, args, Math.min)
      // Signal => Signal
      case SignalStream(_, IntType, _, _) :: Nil =>
        aggregateSignal[StreamInt](function.name, args, Math.min)
      case SignalStream(_, FloatType, _, _) :: Nil =>
        aggregateSignal[StreamFloat](function.name, args, Math.min)

      case _ => throw UnexpectedArgsException(args)
    }

    case "sum" => args match {
      case EventStream(_, IntType, _) :: Nil =>
        aggregateEvent[StreamInt](function.name, args :+ ConstantStream(IntType, 0), _+_)
      case EventStream(_, FloatType, _) :: Nil =>
        aggregateEvent[StreamFloat](function.name, args :+ ConstantStream(FloatType, 0.0), _+_)
      case _ => throw UnexpectedArgsException(args)
    }

    case "out" => args match {
      case (stream: Stream) :: SignalStream(_, StringType, name: String, _) :: Nil =>
        stream match {
          case s: SignalStream => SignalStream(name, s.valueType, s.initialValue, s.values)
          case s: EventStream => EventStream(name, s.valueType, s.values)
        }
      case _ => throw UnexpectedArgsException(args)
    }

    case _ => throw UnknownFunctionException(function)
  }

  def aggregateEvent[A](name: String, args: Seq[Any], fn: (A, A) => A) = args match {
    case EventStream(_, leftType, valuesAny)
      :: SignalStream(_, startType, startValueAny, _)
      :: Nil if leftType == startType => {
      val values = valuesAny.asInstanceOf[Seq[(Int, A)]]
      val startValue = startValueAny.asInstanceOf[A]
      val results = values.foldLeft(Seq[(Int, Any)](), startValue)({
        case ((r, previous), (t, v)) => {
          val result = fn(v, previous)
          if (result != previous)
            (r :+ (t, result), result)
          else
            (r, previous)
        }
      })._1
      SignalStream(name, leftType, startValue, results)
    }
    case _ => throw UnexpectedArgsException(args)
  }

  def aggregateSignal[A](name: String, args: Seq[Any], fn: (A, A) => A) = args match {
    case SignalStream(_, valueType, initialValue, values) :: Nil => {
      // Signal aggregation is like event aggregation. The only difference is that the start value is provided by the
      // signal stream's initial value.
      val changeEventStream = EventStream("", valueType, values)
      val initialValueStream = ConstantStream(valueType, initialValue)
      aggregateEvent[A](name, Seq(changeEventStream, initialValueStream), fn)
    }
    case _ => throw UnexpectedArgsException(args)
  }

  /**
   * Combines two signals into a new signal by applying function fn to them. It takes in arbitrary arguments, but
   * will only match if args is a sequence of two [[SignalStream]]. Throws an [[UnexpectedArgsException]] otherwise.
   *
   * @param name name of the resulting signal
   * @param args pass through of the function arguments
   * @param fn   the function to apply
   * @tparam A type of first operand
   * @tparam B type of second operand
   * @tparam C result type
   * @return a new [[SignalStream]] derived from the two arguments
   */
  def combineTwoSignals[A, B, C](name: String, args: Seq[Any], fn: (A, B) => C) = args match {
    case (left: SignalStream) :: (right: SignalStream) :: Nil => {
      val leftInitial = left.initialValue.asInstanceOf[A]
      val rightInitial = right.initialValue.asInstanceOf[B]
      val initialValue = fn(leftInitial, rightInitial)

      var leftValues = left.values.asInstanceOf[Seq[(Int, A)]]
      var rightValues = right.values.asInstanceOf[Seq[(Int, B)]]
      var lastLeft = leftInitial
      var lastRight = rightInitial

      var results = Seq[(Int, C)]()

      while (leftValues.nonEmpty || rightValues.nonEmpty) {
        (leftValues, rightValues) match {
          case ((t1, v1) :: l, (t2, v2) :: r) if t1 < t2 =>
            results :+= (t1, fn(v1, lastRight))
            lastLeft = v1
            leftValues = l

          case ((t1, v1) :: l, (t2, v2) :: r) if t1 > t2 =>
            results :+= (t2, fn(lastLeft, v2))
            lastRight = v2
            rightValues = r

          case ((t1, v1) :: l, (t2, v2) :: r) if t1 == t2 =>
            results :+= (t1, fn(v1, v2))
            lastLeft = v1
            lastRight = v2
            leftValues = l
            rightValues = r

          case ((t1, v1) :: l, Nil) =>
            results :+= (t1, fn(v1, lastRight))
            leftValues = l

          case (Nil, (t2, v2) :: r) =>
            results :+= (t2, fn(lastLeft, v2))
            rightValues = r
        }
      }

      // Remove repeated values (we're always striving for the most compact representation)
      var previous = initialValue
      results = results.foldLeft(Seq[(Int, C)]())({
        case (l, (t, v)) if v == previous => l
        case (l, (t, v)) => {
          previous = v
          l :+ (t, v)
        }
      })

      SignalStream(name, valueTypeFromAny(initialValue), initialValue, results)
    }
    case _ => throw UnexpectedArgsException(args)
  }

  def within(name: String, d1: Int, d2: Int, events: Seq[(Int, Any)]): SignalStream = {
    if (d1 >= d2 || events.isEmpty)
      return SignalStream(name, BoolType, false, Seq())

    var changes = Seq[(Int, StreamBool)]()
    var from = events.head._1 - d2
    var until = events.head._1 - d1

    for ((t, _) <- events) {
      if (until >= t - d2) {
        until = t - d1
      } else {
        changes :+= (from, true)
        changes :+= (until, false)
        from = t - d2
        until = t - d1
      }
    }

    changes :+= (from, true)
    changes :+= (until, false)

    SignalStream(name, BoolType, false, changes)
  }
}
