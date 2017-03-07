package tesslasimulator.shared

object Error {
  trait Error {
    def format: String
  }

  trait DefaultFormatting {
    this: Exception =>
    def format: String = this.getMessage
  }
}
