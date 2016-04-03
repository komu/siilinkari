package siilinkari.runtime

import siilinkari.objects.Value
import siilinkari.types.Type

class PrimitiveFunction(signature: Type.Function, private val func: (List<Value>) -> Value) : Value.Function(signature) {
    override operator fun invoke(args: List<Value>): Value = func(args)
}

/**
 * Creates a single argument primitive function.
 */
inline fun <reified A : Value> fun1(argType: Type, returnType: Type, crossinline func: (A) -> Value): Value.Function {
    val signature = Type.Function(listOf(argType), returnType)
    return PrimitiveFunction(signature) { args -> func(args.single() as A) }
}

inline fun <reified A : Value> fun1(argType: Type, crossinline func: (A) -> Unit): Value.Function =
    fun1<A>(argType, Type.Unit) { arg ->
        func(arg)
        Value.Unit
    }
