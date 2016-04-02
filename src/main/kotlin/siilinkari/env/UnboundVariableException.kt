package siilinkari.env

/**
 * Exception thrown when variable is not found from environment.
 *
 * This should never be thrown since type-checking should find all unbound variables.
 */
class UnboundVariableException(name: String) :
        RuntimeException("unbound variable: $name")
