package siilinkari.vm

class VariableAlreadyBoundException(name: String) : RuntimeException("variable already bound: $name")
