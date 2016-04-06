package siilinkari.optimizer

import siilinkari.translator.BasicBlock
import siilinkari.translator.IR

/**
 * Performs local optimizations to IR by looking at a small window of successive
 * instructions.
 */
fun BasicBlock.peepholeOptimize() {
    do {
        var modified = false
        for (i in opCodes.indices) {
            if (i == 0) continue

            val current = opCodes[i]
            val previous = opCodes[i - 1]
            if (current is IR.LoadLocal && previous is IR.StoreLocal && current.index == previous.index) {
                opCodes[i - 1] = IR.Dup
                opCodes[i] = previous
                modified = true
            }
        }
    } while (modified)
}
