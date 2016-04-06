package siilinkari.parser

import siilinkari.ast.Expression
import siilinkari.ast.Expression.Binary
import siilinkari.ast.RelationalOp
import siilinkari.ast.Statement
import siilinkari.lexer.*
import siilinkari.lexer.Token.*
import siilinkari.lexer.Token.Punctuation.LeftBrace
import java.util.*

/**
 * Parse [code] as [Statement].
 *
 * @throws SyntaxErrorException if parsing fails
 */
fun parseStatement(code: String): Statement =
    parseComplete(code) { it.parseStatement() }

/**
 * Parse [code] as [Expression].
 *
 * @throws SyntaxErrorException if parsing fails
 */
fun parseExpression(code: String): Expression =
    parseComplete(code) { it.parseExpression() }

/**
 * Executes parser on code and verifies that it consumes all input.
 *
 * @throws SyntaxErrorException if the parser fails or if it did not consume all input
 */
private fun <T> parseComplete(code: String, callback: (Parser) -> T): T {
    val parser = Parser(Lexer(code))
    val result = callback(parser)
    parser.expectEnd()
    return result
}

/**
 * A simple recursive descent parser.
 */
private class Parser(lexer: Lexer) {

    private val lexer = LookaheadLexer(lexer)

    /**
     * ```
     * statement ::= if | while | var | statement-list | ident "=" expression | expression
     * ```
     */
    fun parseStatement(): Statement = when (lexer.peekToken().token) {
        Keyword.If      -> parseIf()
        Keyword.While   -> parseWhile()
        Keyword.Var     -> parseVariableDefinition()
        Keyword.Val     -> parseVariableDefinition()
        LeftBrace       -> parseStatementList()
        is Identifier   -> {
            val exp = parseExpression();
            val stmt = if (exp is Expression.Ref && lexer.nextTokenIs(Punctuation.Equal))
                parseAssignTo(exp.name)
            else
                Statement.Exp(exp)
            lexer.expect(Punctuation.Semicolon)
            stmt
        }
        else -> {
            val exp = parseExpression()
            lexer.expect(Punctuation.Semicolon)
            Statement.Exp(exp)
        }
    }

    /**
     * Parses an expression.
     *
     * Expression parsers are separated to different levels to handle precedence
     * of operators correctly. The lower levels bind more tightly that the higher
     * levels.
     *
     * ```
     * expression ::= expression2 (("==" | "!=") expression2)*
     * ```
     */
    fun parseExpression(): Expression {
        var exp = parseExpression2()

        while (lexer.hasMore) {
            val location = lexer.nextTokenLocation()
            when {
                lexer.readNextIf(Operator.EqualEqual) ->
                    exp = Binary.Relational(RelationalOp.Equals, exp, parseExpression2(), location)
                lexer.readNextIf(Operator.NotEqual) ->
                    exp = Binary.Relational(RelationalOp.NotEquals, exp, parseExpression2(), location)
                else ->
                    return exp
            }
        }

        return exp
    }

    /**
     * ```
     * expression2 ::= expression3 (("<" | ">" | "<=" | ">=") expression3)*
     * ```
     */
    private fun parseExpression2(): Expression {
        var exp = parseExpression3()

        while (lexer.hasMore) {
            val location = lexer.nextTokenLocation()
            when {
                lexer.readNextIf(Operator.LessThan) ->
                    exp = Binary.Relational(RelationalOp.LessThan, exp, parseExpression3(), location)
                lexer.readNextIf(Operator.LessThanOrEqual) ->
                    exp = Binary.Relational(RelationalOp.LessThanOrEqual, exp, parseExpression3(), location)
                lexer.readNextIf(Operator.GreaterThan) ->
                    exp = Binary.Relational(RelationalOp.GreaterThan, exp, parseExpression3(), location)
                lexer.readNextIf(Operator.GreaterThanOrEqual) ->
                    exp = Binary.Relational(RelationalOp.GreaterThanOrEqual, exp, parseExpression3(), location)
                else ->
                    return exp
            }
        }

        return exp
    }

    /**
     * ```
     * expression3 ::= expression4 (("+" | "-") expression4)*
     * ```
     */
    private fun parseExpression3(): Expression {
        var exp = parseExpression4()

        while (lexer.hasMore) {
            val location = lexer.nextTokenLocation()
            when {
                lexer.readNextIf(Operator.Plus) ->
                    exp = Binary.Plus(exp, parseExpression4(), location)
                lexer.readNextIf(Operator.Minus) ->
                    exp = Binary.Minus(exp, parseExpression4(), location)
                else ->
                    return exp
            }
        }

        return exp
    }

    /**
     * ```
     * expression4 ::= expression5 (("*" | "/") expression5)*
     * ```
     */
    private fun parseExpression4(): Expression {
        var exp = parseExpression5()

        while (lexer.hasMore) {
            val location = lexer.nextTokenLocation()
            when {
                lexer.readNextIf(Operator.Multiply) ->
                    exp = Binary.Multiply(exp, parseExpression5(), location)
                lexer.readNextIf(Operator.Divide) ->
                    exp = Binary.Divide(exp, parseExpression5(), location)
                else ->
                    return exp
            }
        }

        return exp
    }

    /**
     * ```
     * expression5 ::= expression6 [ '(' args ')']
     * ```
     */
    private fun parseExpression5(): Expression {
        val exp = parseExpression6()

        return if (lexer.nextTokenIs(Punctuation.LeftParen))
            Expression.Call(exp, parseArgumentList())
        else
            exp
    }

    /**
     * ```
     * expression5 ::= identifier | literal | not | "(" expression ")"
     * ```
     */
    private fun parseExpression6(): Expression {
        val (token, location) = lexer.peekToken()

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

    private fun parseVariableDefinition(): Statement {
        val mutable = lexer.nextTokenIs(Keyword.Var)
        val location = if (mutable) lexer.expect(Keyword.Var) else lexer.expect(Keyword.Val)

        val variable = parseName().first
        lexer.expect(Punctuation.Equal)
        val expression = parseExpression()
        lexer.expect(Punctuation.Semicolon);

        return Statement.Var(variable, expression, mutable, location)
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
        val location = lexer.nextTokenLocation()
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
        val exp = parseExpression5()

        return Expression.Not(exp, location)
    }

    private fun parseIdentifier(): Expression {
        val (name, location) = parseName()

        return Expression.Ref(name, location)
    }

    private fun parseArgumentList(): List<Expression> =
        inParens {
            if (lexer.nextTokenIs(Token.Punctuation.RightParen))
                emptyList()
            else {
                val args = ArrayList<Expression>()
                do {
                    args += parseExpression()
                } while (lexer.readNextIf(Token.Punctuation.Comma))
                args
            }
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
            val (token, location) = lexer.peekToken()
            fail(location, "expected end, but got $token")
        }
    }
}
