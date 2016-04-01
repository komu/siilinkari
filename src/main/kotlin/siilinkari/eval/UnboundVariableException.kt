package siilinkari.eval

class UnboundVariableException(name: String) : RuntimeException("unbound variable: $name")
