package siilinkari.runtime

import siilinkari.objects.Value
import siilinkari.types.Type
import siilinkari.vm.Evaluator

fun registerRuntimeFunctions(evaluator: Evaluator) {
    evaluator.bindFunction(fun1<Value.String>("println", Type.String, ::println))
    evaluator.bindFunction(fun1<Value.String>("error", Type.String, ::error))
    evaluator.bindFunction(fun2("stringArrayOfSize", Type.Int, Type.String, Type.Array(Type.String), { s: Value.Integer, i: Value.String -> arrayOfSize(s, Type.String, i) }))
    evaluator.bindFunction(fun1("stringArrayLength", Type.Array(Type.String), Type.Int, ::arrayLength))
    evaluator.bindFunction(fun2("stringArrayGet", Type.Array(Type.String), Type.Int, Type.String, ::arrayGet))
    evaluator.bindFunction(fun3("stringArraySet", Type.Array(Type.String), Type.Int, Type.String, Type.Unit, ::arraySet))
}

private fun arrayOfSize(size: Value.Integer, elementType: Type, initialValue: Value): Value.Array =
    Value.Array(kotlin.Array(size.value) { initialValue }, elementType)

private fun arrayLength(array: Value.Array): Value.Integer =
    Value.Integer(array.elements.size)

private fun arrayGet(array: Value.Array, index: Value.Integer): Value =
    array.elements[index.value]

private fun arraySet(array: Value.Array, index: Value.Integer, value: Value): Value.Unit {
    array.elements[index.value] = value
    return Value.Unit
}

private fun Evaluator.bindFunction(func: Value.Function) {
    bind(func.name, func, mutable = false)
}
