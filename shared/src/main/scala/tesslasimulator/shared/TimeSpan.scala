package tesslasimulator.shared

case class TimeSpan(start: Int, end: Int) {
  require(start < end, "Start of a time span must be before it's end")

  def disjoint(other: TimeSpan): Boolean = other.end <= start || other.start >= end

  def cut(other: TimeSpan): Seq[TimeSpan] = {
    other match {
      case noOverlap if disjoint(other) =>
        Seq(this)
      case fullOverlap if other.start <= start && other.end >= end =>
        Seq()
      case fullContainment if other.start > start && other.end < end =>
        Seq(TimeSpan(start, other.start), TimeSpan(other.end, end))
      case cutLeftSide if other.start <= start =>
        Seq(TimeSpan(other.end, end))
      case cutRightSide if other.end >= end =>
        Seq(TimeSpan(start, other.start))
    }
  }
}