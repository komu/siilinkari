package siilinkari.runtime

import siilinkari.objects.Value
import siilinkari.types.Type

/**
 * Creates a single argument primitive function.
 */
inline fun <reified A : Value> fun1(name: String, argType: Type, returnType: Type, crossinline func: (A) -> Value): Value.Function {
    val signature = Type.Function(listOf(argType), returnType)
    return Value.Function.Native(name, signature) { args -> func(args.single() as A) }
}

inline fun <reified A : Value> fun1(name: String, argType: Type, crossinline func: (A) -> Unit): Value.Function =
    fun1<A>(name, argType, Type.Unit) { arg ->
        func(arg)
        Value.Unit
    }

inline fun <reified A : Value> fun2(name: String, argType: Type, returnType: Type, crossinline func: (A, A) -> Value): Value.Function {
    val signature = Type.Function(listOf(argType, argType), returnType)
    return Value.Function.Native(name, signature) { args ->
        check(args.size == 2)
        func(args[0] as A, args[1] as A)
    }
}
