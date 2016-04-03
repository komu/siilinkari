package siilinkari.runtime

import siilinkari.objects.Value
import siilinkari.types.Type

/**
 * Creates a single argument primitive function.
 */
inline fun <reified A : Value> fun1(argType: Type, returnType: Type, crossinline func: (A) -> Value): Value.PrimitiveFunction {
    val signature = Type.Function(listOf(argType), returnType)
    return Value.PrimitiveFunction(signature) { args -> func(args.single() as A) }
}

inline fun <reified A : Value> fun1(argType: Type, crossinline func: (A) -> Unit): Value.PrimitiveFunction =
    fun1<A>(argType, Type.Unit) { arg ->
        func(arg)
        Value.Unit
    }
