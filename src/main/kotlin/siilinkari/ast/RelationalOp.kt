package siilinkari.ast

enum class RelationalOp(private val repr: String) {
    Equals("=="),
    NotEquals("!="),
    LessThan("<"),
    LessThanOrEqual("<="),
    GreaterThan(">"),
    GreaterThanOrEqual(">=");

    override fun toString() = repr
}
