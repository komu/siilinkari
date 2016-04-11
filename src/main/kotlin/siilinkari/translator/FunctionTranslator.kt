package siilinkari.translator

import siilinkari.ast.FunctionDefinition
import siilinkari.env.GlobalStaticEnvironment
import siilinkari.optimizer.optimize
import siilinkari.types.Type
import siilinkari.types.expectAssignableTo
import siilinkari.types.typeCheck
import siilinkari.vm.CodeSegment

/**
 * Creates a callable function from given expression.
 */
class FunctionTranslator(val env: GlobalStaticEnvironment) {
    fun translateFunction(func: FunctionDefinition): Pair<Type.Function, CodeSegment> {
        val typedExp = func.body.typeCheck(env.newScope(func.args)).optimize()

        if (func.returnType != null)
            typedExp.expectAssignableTo(func.returnType, func.body.location)

        val basicBlocks = typedExp.translateToIR()

        basicBlocks.optimize()

        basicBlocks.start.prepend(IR.Enter(basicBlocks.frameSize))
        basicBlocks.end += IR.Leave(func.args.size)
        basicBlocks.end += IR.Ret

        return Pair(Type.Function(func.args.map { it.second }, typedExp.type), basicBlocks.translateToCode())
    }
}
