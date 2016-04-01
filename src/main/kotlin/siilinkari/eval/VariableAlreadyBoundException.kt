package siilinkari.eval

class VariableAlreadyBoundException(name: String) : RuntimeException("variable already bound: $name")
