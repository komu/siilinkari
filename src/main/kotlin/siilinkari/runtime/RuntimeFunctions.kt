package siilinkari.runtime

import siilinkari.objects.Value
import siilinkari.types.Type
import siilinkari.vm.Evaluator

fun registerRuntimeFunctions(evaluator: Evaluator) {
    evaluator.bind("println", fun1<Value.String>(Type.String, ::println), mutable = false)
    evaluator.bind("error", fun1<Value.String>(Type.String, ::error), mutable = false)
}
