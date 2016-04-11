package siilinkari.vm

import siilinkari.ast.FunctionDefinition
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.lexer.LookaheadLexer
import siilinkari.lexer.Token.Keyword
import siilinkari.objects.Value
import siilinkari.optimizer.optimize
import siilinkari.parser.parseExpression
import siilinkari.parser.parseFunctionDefinition
import siilinkari.parser.parseFunctionDefinitions
import siilinkari.translator.FunctionTranslator
import siilinkari.translator.IR
import siilinkari.translator.translateToCode
import siilinkari.translator.translateToIR
import siilinkari.types.Type
import siilinkari.types.TypedExpression
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
    var trace = false
    var optimize = true

    /**
     * Binds a global name to given value.
     */
    fun bind(name: String, value: Value, mutable: Boolean = true) {
        val binding = globalTypeEnvironment.bind(name, value.type, mutable)
        globalData[binding.index] = value
    }

    /**
     * Evaluates code which can either be a definition, statement or expression.
     * If the code represented an expression, returns its value. Otherwise [Value.Unit] is returned.
     */
    fun evaluate(code: String): EvaluationResult {
        if (LookaheadLexer(code).nextTokenIs(Keyword.Fun)) {
            val definition = parseFunctionDefinition(code)
            bindFunction(definition)
            return EvaluationResult(Value.Unit, Type.Unit)

        } else {
            val exp = parseExpression(code).typeCheck(globalTypeEnvironment)
            val segment = translate(exp)
            return EvaluationResult(evaluateSegment(segment), exp.type)
        }
    }

    /**
     * Loads contents of given file.
     */
    fun loadResource(file: String) {
        val source = javaClass.classLoader.getResource(file).openStream().use { it.reader().readText() }
        val defs = parseFunctionDefinitions(source, file)

        for (def in defs)
            bindFunction(def)
    }

    /**
     * Returns the names of all global bindings.
     */
    fun bindingsNames(): Set<String> =
        globalTypeEnvironment.bindingNames()

    /**
     * Translates given code to opcodes and returns string representation of the opcodes.
     */
    fun dump(code: String): String =
        translate(parseExpression(code).typeCheck(globalTypeEnvironment)).toString()

    /**
     * Compiles and binds a global function.
     */
    private fun bindFunction(func: FunctionDefinition) {
        // We have to create the binding into global environment before calling createFunction
        // because the function might want to call itself recursively. But if createFunction fails
        // (most probably to type-checking), we need to unbind the binding.
        var binding = func.returnType?.let { returnType ->
            globalTypeEnvironment.bind(func.name, Type.Function(func.args.map { it.second }, returnType), mutable = false)
        }
        try {
            val (signature, code) = functionTranslator.translateFunction(func)
            val address = globalCode.addRelocated(code)
            if (binding != null)
                globalTypeEnvironment.unbind(func.name)

            binding = globalTypeEnvironment.bind(func.name, signature, mutable = false)
            globalData[binding.index] = Value.Function.Compound(func.name, signature, address)
        } catch (e: Exception) {
            if (binding != null)
                globalTypeEnvironment.unbind(func.name)
            throw e
        }
    }

    /**
     * Translates code to opcodes.
     */
    private fun translate(exp: TypedExpression): CodeSegment {
        val optExp = if (optimize) exp.optimize() else exp
        val blocks = optExp.translateToIR()

        if (optimize)
            blocks.optimize()

        blocks.end += IR.Quit

        return blocks.translateToCode()
    }

    /**
     * Evaluates given code segment.
     */
    private fun evaluateSegment(segment: CodeSegment): Value {
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
                OpCode.PushUnit         -> state.push(Value.Unit)
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
