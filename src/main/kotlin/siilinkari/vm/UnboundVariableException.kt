package siilinkari.vm

class UnboundVariableException(name: String) : RuntimeException("unbound variable: $name")
