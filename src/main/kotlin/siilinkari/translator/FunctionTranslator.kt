package siilinkari.translator

import siilinkari.ast.FunctionDefinition
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.optimizer.optimize
import siilinkari.types.typeCheckExpected
import siilinkari.vm.CodeSegment

/**
 * Creates a callable function from given expression.
 */
class FunctionTranslator(val env: GlobalStaticEnvironment) {
    fun translateFunction(func: FunctionDefinition): CodeSegment {
        val typedExp = func.body.typeCheckExpected(func.returnType, env.newScope(func.args)).optimize()

        val basicBlocks = typedExp.translateToIR()

        basicBlocks.optimize()

        basicBlocks.start.prepend(IR.Enter(basicBlocks.frameSize))
        basicBlocks.end += IR.Leave(func.argumentCount)
        basicBlocks.end += IR.Ret

        return basicBlocks.translateToCode()
    }
}
