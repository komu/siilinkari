package siilinkari.vm

import siilinkari.ast.FunctionDefinition
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.lexer.LookaheadLexer
import siilinkari.lexer.SyntaxErrorException
import siilinkari.lexer.Token.Keyword
import siilinkari.objects.Value
import siilinkari.optimizer.optimize
import siilinkari.parser.parseExpression
import siilinkari.parser.parseFunctionDefinition
import siilinkari.parser.parseStatement
import siilinkari.translator.FunctionTranslator
import siilinkari.translator.Translator
import siilinkari.types.TypedStatement
import siilinkari.types.type
import siilinkari.types.typeCheck

/**
 * Evaluator for opcodes.
 *
 * @see OpCode
 */
class Evaluator {
    private val globalData = DataSegment()
    private val globalTypeEnvironment = GlobalStaticEnvironment()
    private val globalCode = CodeSegment.Builder()
    private val functionTranslator = FunctionTranslator(globalTypeEnvironment)
    var trace = false;

    /**
     * Binds a global name to given value.
     */
    fun bind(name: String, value: Value, mutable: Boolean = true) {
        val binding = globalTypeEnvironment.bind(name, value.type, mutable)
        globalData[binding.index] = value
    }

    /**
     * Compiles and binds a global function.
     */
    fun bindFunction(func: FunctionDefinition) {
        // We have to create the binding into global environment before calling createFunction
        // because the function might want to call itself recursively. But if createFunction fails
        // (most probably to type-checking), we need to unbind the binding.
        val binding = globalTypeEnvironment.bind(func.name, func.signature, mutable = false)
        try {
            val code = functionTranslator.translateFunction(func)
            val address = globalCode.addRelocated(code)
            globalData[binding.index] = Value.Function.Compound(func.name, func.signature, address)
        } catch (e: Exception) {
            globalTypeEnvironment.unbind(func.name)
            throw e
        }
    }

    fun evaluateReplLine(code: String): Value {
        if (LookaheadLexer(code).nextTokenIs(Keyword.Fun)) {
            val definition = parseFunctionDefinition(code)
            bindFunction(definition)
            return Value.Unit
        }

        try {
            parseExpression(code)
            return evaluateExpression(code)
        } catch (e: SyntaxErrorException) {
            evaluateStatement(code)
            return Value.Unit
        }
    }

    /**
     * Evaluates statement of code. If the statement is an expression
     * statement, returns its value. Otherwise returns [Value.Unit].
     */
    fun evaluateStatement(code: String): Value {
        val translated = translate(code)

        return evaluateSegment(translated)
    }

    /**
     * Evaluates an expression and returns its value.
     */
    fun evaluateExpression(code: String): Value {
        val exp = parseExpression(code)
        val typedExp = exp.typeCheck(globalTypeEnvironment).optimize()
        val translated = CodeSegment.Builder()

        val translator = Translator()
        translator.translateExpression(typedExp)
        translator.optimize()
        translator.translateTo(translated)

        translated += OpCode.Quit

        return evaluateSegment(translated)
    }

    fun bindingsNames(): Set<String> =
        globalTypeEnvironment.bindingNames()

    /**
     * Translates given code to opcodes and returns string representation of the opcodes.
     */
    fun dump(code: String): String =
        translate(code).toString()

    /**
     * Translates code to opcodes.
     */
    private fun translate(code: String): CodeSegment.Builder {
        val stmt = parseStatement(code)
        val typedStmt = stmt.typeCheck(globalTypeEnvironment).optimize()

        val translator = Translator()
        if (typedStmt is TypedStatement.Exp)
            translator.translateExpression(typedStmt.expression)
        else
            translator.translateStatement(typedStmt)

        translator.optimize()

        val translated = CodeSegment.Builder()
        translator.translateTo(translated)
        translated += OpCode.Quit
        return translated
    }

    /**
     * Evaluates given code segment.
     */
    private fun evaluateSegment(segment: CodeSegment.Builder): Value {
        // Relocate code to be evaluated after the global code into a single segment.
        val codeBuilder = CodeSegment.Builder(globalCode)
        val startAddress = codeBuilder.addRelocated(segment)
        val code = codeBuilder.build()
        val initialStackPointer = codeBuilder.frameSize

        val state = ThreadState()
        state.pc = startAddress
        state.fp = 0
        state.sp = initialStackPointer

        evalLoop@while (true) {
            val op = code[state.pc++]
            if (trace)
                println(op)
            when (op) {
                OpCode.Pop              -> state.popValue()
                OpCode.Dup              -> state.dup()
                OpCode.Not              -> state.push(!state.pop<Value.Bool>())
                OpCode.Add              -> state.evalBinary<Value.Integer, Value.Integer> { l, r -> l + r }
                OpCode.Subtract         -> state.evalBinary<Value.Integer, Value.Integer> { l, r -> l - r }
                OpCode.Multiply         -> state.evalBinary<Value.Integer, Value.Integer> { l, r -> l * r }
                OpCode.Divide           -> state.evalBinary<Value.Integer, Value.Integer> { l, r -> l / r }
                OpCode.Equal            -> state.evalBinary<Value, Value> { l, r -> Value.Bool(l == r) }
                OpCode.LessThan         -> state.evalBinary<Value, Value> { l, r -> Value.Bool(l.lessThan(r)) }
                OpCode.LessThanOrEqual  -> state.evalBinary<Value, Value> { l, r -> Value.Bool(l == r || l.lessThan(r)) }
                OpCode.ConcatString     -> state.evalBinary<Value.String, Value> { l, r -> l + r }
                is OpCode.Push          -> state.push(op.value)
                is OpCode.Jump          -> state.pc = op.address
                is OpCode.JumpIfFalse   -> if (!state.pop<Value.Bool>().value) state.pc = op.address
                is OpCode.Enter         -> state.enterFrame(op.frameSize)
                is OpCode.Leave         -> state.leaveFrame(op.paramCount)
                is OpCode.LoadLocal     -> state.push(state[op.offset])
                is OpCode.LoadGlobal    -> state.push(globalData[op.offset])
                is OpCode.LoadArgument  -> state.push(state.loadArgument(op.offset))
                is OpCode.StoreLocal    -> state[op.offset] = state.popValue()
                is OpCode.StoreGlobal   -> globalData[op.offset] = state.popValue()
                is OpCode.Call          -> evalCall(state)
                OpCode.Ret              -> state.pc = state.pop<Value.Pointer.Code>().value
                OpCode.Quit             -> break@evalLoop
                else                    -> error("unknown opcode: $op")
            }
        }

        return if (state.sp > initialStackPointer) state.popValue() else Value.Unit
    }

    private fun evalCall(state: ThreadState) {
        val func = state.pop<Value.Function>()

        when (func) {
            is Value.Function.Compound -> {
                state.push(Value.Pointer.Code(state.pc))
                state.pc = func.address
            }
            is Value.Function.Native ->
                state.push(func(state.popValues(func.argumentCount)))
        }
    }
}
