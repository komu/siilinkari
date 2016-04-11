package siilinkari.translator

/**
 * Internal error thrown when we detect the translator or some optimization
 * has left the IR stack use in invalid state.
 *
 * It's always a bug in compiler if this is thrown: never an error in user program.
 */
class InvalidStackUseException(message: String) : RuntimeException(message)
