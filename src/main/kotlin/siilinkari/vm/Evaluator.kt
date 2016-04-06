package siilinkari.vm

import siilinkari.env.GlobalStaticEnvironment
import siilinkari.objects.Value
import siilinkari.optimizer.optimize
import siilinkari.parser.parseExpression
import siilinkari.parser.parseStatement
import siilinkari.translator.translateTo
import siilinkari.types.Type
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

    /**
     * Binds a global name to given value.
     */
    fun bind(name: String, value: Value, mutable: Boolean = true) {
        val binding = globalTypeEnvironment.bind(name, value.type, mutable)
        globalData[binding.index] = value
    }

    /**
     * Binds a global function using given expression as body.
     */
    fun bindFunction(name: String, args: List<Pair<String, Type>>, code: String) {
        bind(name, createFunctionFromExpression(args, code))
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
        typedExp.translateTo(translated)
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

        val translated = CodeSegment.Builder()
        if (typedStmt is TypedStatement.Exp) {
            typedStmt.expression.translateTo(translated)
        } else {
            typedStmt.translateTo(translated)
        }
        translated += OpCode.Quit
        return translated
    }

    /**
     * Creates a callable function from given expression.
     */
    private fun createFunctionFromExpression(args: List<Pair<String, Type>>, code: String): Value.Function {
        val typedExp = parseExpression(code).typeCheck(globalTypeEnvironment.newScope(args)).optimize()

        val codeSegment = CodeSegment.Builder()
        typedExp.translateTo(codeSegment)

        val argTypes = args.map { it.second }
        val signature = Type.Function(argTypes, typedExp.type)

        val finalCode = CodeSegment.Builder()
        finalCode += OpCode.Enter(codeSegment.frameSize)
        finalCode.addRelocated(codeSegment)
        finalCode += OpCode.Leave(argTypes.size)
        finalCode += OpCode.Ret

        val address = globalCode.addRelocated(finalCode)

        return Value.Function.Compound(signature, address)
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
            when (op) {
                OpCode.Pop              -> state.popValue()
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
                is OpCode.Jump          -> state.pc = op.label.address
                is OpCode.JumpIfFalse   -> if (!state.pop<Value.Bool>().value) state.pc = op.label.address
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
