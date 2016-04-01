package siilinkari.parser

import siilinkari.ast.Expression
import siilinkari.ast.Statement
import siilinkari.lexer.*
import siilinkari.lexer.Token.*
import siilinkari.lexer.Token.Punctuation.LeftBrace
import java.util.*

fun parseStatement(source: String): Statement =
    parseComplete(source) { this.parseStatement() }

fun parseExpression(source: String): Expression =
    parseComplete(source) { this.parseExpression() }

private fun <T> parseComplete(source: String, callback: Parser.() -> T): T {
    val parser = Parser(source)
    val result = parser.callback()
    parser.expectEnd()
    return result
}

class Parser private constructor(lexer: Lexer) {

    private val lexer = LookaheadLexer(lexer)

    constructor(source: String): this(Lexer(source)) {
    }

    fun parseStatement(): Statement = when (lexer.peekToken()) {
        Keyword.If      -> parseIf()
        Keyword.While   -> parseWhile()
        Keyword.Var     -> parseVar()
        LeftBrace       -> parseStatementList()
        else -> {
            val exp = parseExpression()

            val stmt = if (exp is Expression.Ref && lexer.nextTokenIs(Punctuation.Equal))
                parseAssignTo(exp.name)
            else
                Statement.Exp(exp)

            lexer.expect(Punctuation.Semicolon)
            stmt
        }
    }

    fun parseExpression(): Expression {
        var exp = parseExpression2()

        while (lexer.hasMore) {
            val location = lexer.peekTokenInfo().location
            when {
                lexer.readNextIf(Operator.Equals) ->
                    exp = Expression.Binary.Equals(exp, parseExpression2(), location)
                lexer.readNextIf(Operator.NotEquals) ->
                    exp = Expression.Binary.NotEquals(exp, parseExpression2(), location)
                else ->
                    return exp
            }
        }

        return exp
    }

    private fun parseExpression2(): Expression {
        var exp = parseExpression3()

        while (lexer.hasMore) {
            val location = lexer.peekTokenInfo().location
            when {
                lexer.readNextIf(Operator.Plus) ->
                    exp = Expression.Binary.Plus(exp, parseExpression3(), location)
                lexer.readNextIf(Operator.Minus) ->
                    exp = Expression.Binary.Minus(exp, parseExpression3(), location)
                else ->
                    return exp
            }
        }

        return exp
    }

    private fun parseExpression3(): Expression {
        val (token, location) = lexer.peekTokenInfo()

        return when (token) {
            is Token.Identifier      -> parseIdentifier()
            is Token.Literal         -> parseLiteral()
            is Operator.Not          -> parseNot()
            is Punctuation.LeftParen -> inParens { parseExpression() }
            else                     -> fail(location, "unexpected token $token")
        }
    }

    private fun parseAssignTo(variable: String): Statement {
        val location = lexer.expect(Punctuation.Equal)
        val rhs = parseExpression()

        return Statement.Assign(variable, rhs, location)
    }

    private fun parseVar(): Statement {
        val location = lexer.expect(Keyword.Var)
        val variable = parseName().first
        lexer.expect(Punctuation.Equal)
        val expression = parseExpression()
        lexer.expect(Punctuation.Semicolon);

        return Statement.Var(variable, expression, location)
    }

    private fun parseIf(): Statement {
        val location = lexer.expect(Keyword.If)
        val condition = inParens { parseExpression() }
        val consequent = parseStatement()
        val alternative = if (lexer.readNextIf(Keyword.Else)) parseStatement() else null

        return Statement.If(condition, consequent, alternative, location)
    }

    private fun parseWhile(): Statement {
        val location = lexer.expect(Keyword.While)
        val condition = inParens { parseExpression() }
        val body = parseStatement()

        return Statement.While(condition, body, location)
    }

    private fun parseStatementList(): Statement {
        val location = lexer.peekTokenInfo().location
        return inBraces {
            val statements = ArrayList<Statement>()
            while (!lexer.nextTokenIs(Punctuation.RightBrace))
                statements += parseStatement()

            Statement.StatementList(statements, location)
        }
    }

    private fun parseLiteral(): Expression.Lit {
        val (token, location) = lexer.readExpected<Token.Literal>()

        return Expression.Lit(token.value, location)
    }

    private fun parseNot(): Expression {
        val location = lexer.expect(Operator.Not)
        val exp = parseExpression3()

        return Expression.Not(exp, location)
    }

    private fun parseIdentifier(): Expression {
        val (name, location) = parseName()

        return Expression.Ref(name, location)
    }

    private fun parseName(): Pair<String, SourceLocation> {
        val (token, location) = lexer.readExpected<Token.Identifier>()

        return Pair(token.name, location)
    }

    private inline fun <T> inParens(parser: () -> T): T =
        between(Punctuation.LeftParen, Punctuation.RightParen, parser)

    private inline fun <T> inBraces(parser: () -> T): T =
        between(Punctuation.LeftBrace, Punctuation.RightBrace, parser)

    private inline fun <T> between(left: Token, right: Token, parser: () -> T): T {
        lexer.expect(left)
        val value = parser()
        lexer.expect(right)
        return value
    }

    private fun fail(location: SourceLocation, message: String): Nothing =
        throw SyntaxErrorException(message, location)

    fun expectEnd() {
        if (lexer.hasMore) {
            val (token, location) = lexer.peekTokenInfo()
            fail(location, "expected end, but got $token")
        }
    }
}
