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

inline fun <reified A1 : Value, reified A2 : Value> fun2(name: String, argType1: Type, argType2: Type, returnType: Type, crossinline func: (A1, A2) -> Value): Value.Function {
    val signature = Type.Function(listOf(argType1, argType2), returnType)
    return Value.Function.Native(name, signature) { args ->
        check(args.size == 2)
        func(args[0] as A1, args[1] as A2)
    }
}

inline fun <reified A : Value> fun2(name: String, argType: Type, returnType: Type, crossinline func: (A, A) -> Value): Value.Function {
    return fun2(name, argType, argType, returnType, func)
}

inline fun <reified A1 : Value, reified A2 : Value, reified A3 : Value> fun3(name: String, argType1: Type, argType2: Type, argType3: Type, returnType: Type, crossinline func: (A1, A2, A3) -> Value): Value.Function {
    val signature = Type.Function(listOf(argType1, argType2, argType3), returnType)
    return Value.Function.Native(name, signature) { args ->
        check(args.size == 3)
        func(args[0] as A1, args[1] as A2, args[2] as A3)
    }
}
