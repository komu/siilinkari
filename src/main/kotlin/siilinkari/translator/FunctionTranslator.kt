package siilinkari.translator

import siilinkari.ast.FunctionDefinition
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.optimizer.optimize
import siilinkari.types.typeCheckExpected
import siilinkari.vm.CodeSegment
import siilinkari.vm.OpCode

/**
 * Creates a callable function from given expression.
 */
class FunctionTranslator(val env: GlobalStaticEnvironment) {
    fun translateFunction(func: FunctionDefinition): CodeSegment {
        val typedExp = func.body.typeCheckExpected(func.returnType, env.newScope(func.args)).optimize()

        val functionBodyCode = CodeSegment.Builder()
        typedExp.translateTo(functionBodyCode)

        val finalCode = CodeSegment.Builder()
        finalCode += OpCode.Enter(functionBodyCode.frameSize)
        finalCode.addRelocated(functionBodyCode)
        finalCode += OpCode.Leave(func.argumentCount)
        finalCode += OpCode.Ret

        return finalCode.build()
    }
}
