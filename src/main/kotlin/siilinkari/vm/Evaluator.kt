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
import siilinkari.translator.translateToCode
import siilinkari.translator.translateToIR
import siilinkari.types.Type
import siilinkari.types.TypedExpression
import siilinkari.types.typeCheck
import siilinkari.vm.OpCode.*
import siilinkari.vm.OpCode.Binary.*

/**
 * Evaluator for opcodes.
 *
 * @see OpCode
 */
class Evaluator {
    private val globalData = DataSegment()
    private val globalTypeEnvironment = GlobalStaticEnvironment()
    private var globalCode = CodeSegment()
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
            val (segment, type) = translate(code)
            return EvaluationResult(evaluateSegment(segment), type)
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
    fun dump(code: String): String {
        val exp = parseAndTypeCheck(code)

        if (exp is TypedExpression.Ref && exp.type is Type.Function) {
            val value = globalData[exp.binding.index] as Value.Function
            return when (value) {
                is Value.Function.Compound -> globalCode.getRegion(value.address, value.codeSize).toString()
                is Value.Function.Native   -> "native function $value"
            }
        } else {
            return translate(exp).toString()
        }
    }

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
            val (signature, code) = functionTranslator.translateFunction(func, optimize)
            val (newGlobalCode, address) = globalCode.mergeWithRelocatedSegment(code)
            globalCode = newGlobalCode
            if (binding == null)
                binding = globalTypeEnvironment.bind(func.name, signature, mutable = false)

            globalData[binding.index] = Value.Function.Compound(func.name, signature, address, code.size)
        } catch (e: Exception) {
            if (binding != null)
                globalTypeEnvironment.unbind(func.name)
            throw e
        }
    }

    /**
     * Translates code to opcodes.
     */
    private fun translate(code: String): Pair<CodeSegment, Type> {
        val exp = parseAndTypeCheck(code)
        return Pair(translate(exp), exp.type)
    }

    private fun translate(exp: TypedExpression): CodeSegment {
        val optExp = if (optimize)
            exp.optimize()
        else
            exp

        val blocks = optExp.translateToIR()

        if (optimize)
            blocks.optimize()

        return blocks.translateToCode(0)
    }

    private fun parseAndTypeCheck(code: String) =
        parseExpression(code).typeCheck(globalTypeEnvironment)

    /**
     * Evaluates given code segment.
     */
    private fun evaluateSegment(segment: CodeSegment): Value {
        val (code, startAddress) = globalCode.mergeWithRelocatedSegment(segment)
        val quitPointer = Value.Pointer.Code(-1)

        val state = ThreadState()
        state.pc = startAddress
        state.fp = 0
        state[0] = quitPointer

        evalLoop@while (true) {
            val op = code[state.pc]
            if (trace)
                println("${state.pc.toString().padStart(4)}: ${op.toString().padEnd(40)} [fp=${state.fp}]")

            state.pc++
            when (op) {
                is Not              -> state[op.target] = !(state[op.source] as Value.Bool)
                is Add              -> state.evalBinary(op) { l, r -> l + r }
                is Subtract         -> state.evalBinary(op) { l, r -> l - r }
                is Multiply         -> state.evalBinary(op) { l, r -> l * r }
                is Divide           -> state.evalBinary(op) { l, r -> l / r }
                is Equal            -> state.evalBinaryBool(op) { l, r -> l == r }
                is LessThan         -> state.evalBinaryBool(op) { l, r -> l.lessThan(r) }
                is LessThanOrEqual  -> state.evalBinaryBool(op) { l, r -> l == r || l.lessThan(r) }
                is ConcatString     -> state.evalBinary(op) { l, r-> l + r }
                is LoadConstant     -> state[op.target] = op.value
                is Copy             -> state[op.target] = state[op.source]
                is LoadGlobal       -> state[op.target] = globalData[op.sourceGlobal]
                is StoreGlobal      -> globalData[op.targetGlobal] = state[op.source]
                is Jump             -> state.pc = op.address
                is JumpIfFalse      -> if (!(state[op.sp] as Value.Bool).value) state.pc = op.address
                is Call             -> evalCall(op, state)
                is RestoreFrame     -> state.fp -= op.sp
                is Ret           -> {
                    val returnAddress = state[op.returnAddressPointer]
                    state[0] = state[op.valuePointer]
                    if (returnAddress == quitPointer)
                        break@evalLoop
                    state.pc = (returnAddress as Value.Pointer.Code).value
                }
                Nop                 -> {}
                else                    -> error("unknown opcode: $op")
            }
        }

        return state[0]
    }

    private fun evalCall(op: Call, state: ThreadState) {
        val func = state[op.offset] as Value.Function

        state.fp += op.offset - op.argumentCount
        when (func) {
            is Value.Function.Compound -> {
                state[op.argumentCount] = Value.Pointer.Code(state.pc)
                state.pc = func.address
            }
            is Value.Function.Native -> {
                val args = state.getArgs(func.argumentCount)
                state[0] = func(args)
            }
        }
    }
}
