package siilinkari.optimizer

import siilinkari.translator.BasicBlock
import siilinkari.translator.IR
import java.util.*

fun BasicBlock.peepholeOptimize() {

    val ops = ArrayList<IR>(opCodes)

    opCodes.clear()

    ops.forEachIndexed { i, current ->
        opCodes.add(current)

        if (i > 0) {
            val previous = ops[i - 1]
            if (current is IR.LoadLocal && previous is IR.StoreLocal && current.index == previous.index) {
                opCodes[i - 1] = IR.Dup
                opCodes[i] = previous
            }
        }
    }
}
