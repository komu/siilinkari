package siilinkari.lexer

/**
 * Represents a location in source code.
 *
 * In addition to actual location (file name and position in file) the class also
 * contains reference to the source code in the line, allowing easy printing of
 * context for error messages.
 */
data class SourceLocation(val file: String, val line: Int, val column: Int, val lineText: String) {

    override fun toString() = "[$file:$line:$column]"

    fun toLongString(): String {
        val prefix = "[$file:$line:$column] "
        val indent = " ".repeat(column - 1 + prefix.length)

        return "$prefix$lineText\n$indent^\n"
    }
}
