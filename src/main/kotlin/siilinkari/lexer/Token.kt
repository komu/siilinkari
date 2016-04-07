package siilinkari.lexer

import siilinkari.objects.Value

/**
 * Tokens are the indivisible building blocks of source code.
 *
 * [Lexer] analyzes the source string to return tokens to be consumed by the parser.
 *
 * Some tokens are singleton values: e.g. when encountering `if` in the source code,
 * the lexer will return simply [Token.Keyword.If]. Other tokens contain information about
 * the value read: e.g. for source code `123`, the lexer will return [Token.Literal],
 * with its `value` set to integer `123`.
 *
 * @see TokenInfo
 */
sealed class Token {

    /**
     * Identifier such as variable, method or class name.
     */
    class Identifier(val name: String): Token() {
        override fun toString() = "[Identifier $name]"
        override fun equals(other: Any?) = other is Identifier && name == other.name
        override fun hashCode(): Int = name.hashCode()
    }

    /**
     * Literal value, e.g. `42`, `"foo"`, or `true`.
     */
    class Literal(val value: Value) : Token() {
        override fun toString() = "[Literal ${value.repr()}]"
        override fun equals(other: Any?) = other is Literal && value == other.value
        override fun hashCode(): Int = value.hashCode()
    }

    /**
     * Reserved word in the language.
     */
    sealed class Keyword(private val name: String) : Token() {

        override fun toString() = name

        object Else : Keyword("else")
        object Fun : Keyword("fun")
        object If : Keyword("if")
        object Var : Keyword("var")
        object Val : Keyword("val")
        object While : Keyword("while")
    }

    /**
     * Operators.
     */
    sealed class Operator(private val name: String) : Token() {

        override fun toString() = name

        object Plus : Operator("+")
        object Minus : Operator("-")
        object Multiply : Operator("*")
        object Divide : Operator("/")
        object EqualEqual : Operator("==")
        object NotEqual : Operator("!=")
        object Not : Operator("!")
        object LessThan : Operator("<")
        object GreaterThan : Operator(">")
        object LessThanOrEqual : Operator("<=")
        object GreaterThanOrEqual : Operator(">=")
    }

    /**
     * General punctuation.
     */
    sealed class Punctuation(private val name: String) : Token() {

        override fun toString() = "'$name'"

        object LeftParen : Punctuation("(")
        object RightParen : Punctuation(")")
        object LeftBrace : Punctuation("{")
        object RightBrace : Punctuation("}")
        object Equal : Punctuation("=")
        object Colon : Punctuation(":")
        object Semicolon : Punctuation(";")
        object Comma : Punctuation(",")
    }
}
