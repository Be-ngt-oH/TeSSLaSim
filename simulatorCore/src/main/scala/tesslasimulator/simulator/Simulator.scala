package tesslasimulator.simulator

import scala.util.{Failure, Success, Try}
import de.uni_luebeck.isp.tessla.FunctionGraph
import tesslasimulator.shared._
import tesslasimulator.shared.Error._

class Simulator(val scenarioStreams: Map[String, Stream], val functionGraph: FunctionGraph) {
  import tesslasimulator.simulator.Error.ScenarioTesslaSpecMismatchError
  import de.uni_luebeck.isp.tessla._
  import functionGraph.Node

  def simulate: Either[Map[String, Stream], Seq[Error]] = {
    val badStreams = findInputStreamMismatches(scenarioStreams, functionGraph)
    if (badStreams.nonEmpty)
      return Right(badStreams.map(_._2).map(ScenarioTesslaSpecMismatchError.apply))

    val sinks = functionGraph.nodes.values.filter(x => x.function.name == "out")

    Left(scenarioStreams ++ sinks.map(evaluateNode(_, scenarioStreams)).map(s => s.name -> s))
  }

  def evaluateNode(node: Node, scenarioStreams: Map[String, Stream]): Stream = {
    // TODO: Optimise this by storing already evaluated args (memoization)
    val args = node.args.map(_.node).map(evaluateNode(_, scenarioStreams))
    FunctionEvaluator.evaluateFunction(node.function, args, scenarioStreams)
  }

  /**
   * Checks if all input streams of a [[FunctionGraph]] are defined in a given scenario and have the same types.
   * Returns a list of all input streams that don't satisfy this requirement and an attached error string providing
   * more detail on the reasons.
   *
   * @param scenarioStreams the compiled scenario
   * @param functionGraph the function graph
   * @return list of all bad input streams
   */
  def findInputStreamMismatches(
      scenarioStreams: Map[String, Stream],
      functionGraph: FunctionGraph): Seq[(InputStream, String)] = {
    def checkInput(input: InputStream): Option[String] = {
      if (!scenarioStreams.isDefinedAt(input.name))
        return Some(s"Unknown input stream ${input.name}.")

      val scenarioStream = scenarioStreams(input.name)

      if (!input.`type`.isInstanceOf[GenericType])
        return Some(s"Stream type of input stream ${input.name} should be Signal<btype> or Events<btype>.")

      val inputType = input.`type`.asInstanceOf[GenericType]

      (inputType.name.toLowerCase, scenarioStream) match {
        case ("signal", s: SignalStream) => // It's good
        case ("events", s: EventStream) => // Also good

        case ("signal", _) =>
          return Some(s"Stream type of input stream ${input.name} is Signal, but defined as Event in scenario.")
        case ("events", _) =>
          return Some(s"Stream type of input stream ${input.name} is Events, but defined as Signal in scenario.")
        case ("event", _) =>
          return Some(s"Stream type of input stream ${input.name} is Event (did you mean Events?)")
        case _ =>
          return Some(s"Stream type of input stream ${input.name} should be Signal<btype> or Events<btype>.")
      }

      if (inputType.args.length > 1)
        return Some(s"Input stream ${input.name} has too many type annotations.")

      if (!inputType.args.head.isInstanceOf[SimpleType])
        return Some(s"Input stream ${input.name} has a too complex type annotation.")

      val inputTypeName = inputType.args.head.asInstanceOf[SimpleType].name

      (inputTypeName.toLowerCase, scenarioStream.valueType) match {
        case ("boolean", BoolType) => // Good,
        case ("int", IntType) => // great,
        case ("float", FloatType) => // awesome,
        case ("string", StringType) => // fabulous.
        case ("unit", NoValueType) => // Cool?
        case (_, b) =>
          return Some(s"Input stream ${input.name} is annotated with $inputTypeName, but defined as $b in scenario.")
      }

      None
    }

    val inputsInGraph: Iterable[InputStream] = functionGraph.nodes.values.
      map(_.function).
      collect({ case i: InputStream => i })

    inputsInGraph.foldLeft(Seq[(InputStream, String)]())((l, input) => {
      checkInput(input) match {
        case Some(errorString) => l :+ (input, errorString)
        case None => l
      }
    })
  }
}

object Simulator {
  import tesslasimulator.simulator.Error._
  import de.uni_luebeck.isp.tessla._

  class CustomTesslaCompiler extends Compiler(false, true) {
    override def applyPasses(src: TesslaSource): Option[AnyRef] = {
      import de.uni_luebeck.isp.tessla.TypeChecker

      try {
        val result = (StateWrapper(src)
        (Parser)
        (DefExtractor)
        (MacroResolver)
        (TypeChecker)
        (AscriptionRemover)
        (SaltConverter)
        (ConstantFolder)).state

        Some(result)
      } catch {
        case x: Diagnostic =>
          this.diagnostic(x)
          None
        case _: Exception =>
          None
      }
    }
  }

  def simulate(scenarioInput: String, tesslaInput: String): Either[Map[String, Stream], Seq[Error]] = {
    def compileScenario(scenarioInput: String): Either[Map[String, Stream], Seq[Error]] = {
      import tesslasimulator.parser._

      var scenarioErrors = Seq[Error]()
      val parseResult = ScenarioParser.parseScenario(scenarioInput)
      if (parseResult.isRight)
        scenarioErrors ++= parseResult.right.get
      if (scenarioErrors.isEmpty)
        scenarioErrors ++= TypeChecker.checkTypes(parseResult.left.get)
      if (scenarioErrors.isEmpty)
        Try[Map[String, Stream]](
          ScenarioCompiler.compileScenarioDescription(parseResult.left.get)
        ) match {
          case Success(scenarioDescription) => return Left(scenarioDescription)
          case Failure(e) => scenarioErrors :+= ScenarioCompilationException(e)
        }

      Right(scenarioErrors)
    }

    def compileTessla(tesslaInput: String): Either[FunctionGraph, Seq[Error]] = {
      val tesslaCompiler = new CustomTesslaCompiler()
      var tesslaErrors = Seq[Error]()
      tesslaCompiler.applyPasses(TesslaSource.fromString(tesslaInput)) match {
        case Some(functionGraph: FunctionGraph) => return Left(functionGraph)
        case _ => {
          tesslaErrors ++= tesslaCompiler.diagnostics.map(d => TesslaDiagnostic(d))
        }
      }

      Right(tesslaErrors)
    }

    (compileScenario(scenarioInput), compileTessla(tesslaInput)) match {
      case (Left(scenarioStreams), Left(functionGraph)) =>
          new Simulator(scenarioStreams, functionGraph).simulate
      case (scenario, tessla) =>
        var errors = Seq[Error]()
        if (scenario.isRight)
          errors ++= scenario.right.get
        if (tessla.isRight)
          errors ++= tessla.right.get

        Right(errors)
    }
  }
}

