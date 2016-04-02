package siilinkari.env

/**
 * Exception thrown when variable is rebound.
 *
 * This should never be thrown since type-checking should find all rebindings.
 */
class VariableAlreadyBoundException(name: String) :
        RuntimeException("variable already bound: $name")
