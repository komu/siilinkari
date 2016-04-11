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

        for (optimizer in optimizers)
            if (optimizer.optimize(this))
                modified = true

    } while (modified)
}

/** List of optimizers to run. */
private val optimizers = listOf(
        RedundantPushUnitPopOptimizer,
        RedundantLoadOptimizer,
        RedundantLoadStoreOptimizer)

/**
 * Base class for peephole optimizers. Optimizers will implement the abstract
 * [optimizeWindow] method which will then get called with each [windowSize] sized
 * window of the [BasicBlock] to optimize.
 */
private abstract class PeepholeOptimizer(val windowSize: Int) {

    init {
        require(windowSize > 0)
    }

    /**
     * Apply optimizations to given [basicBlock].
     *
     * @return True if block was modified
     */
    fun optimize(basicBlock: BasicBlock): Boolean {
        var modified = false

        val opCodes = basicBlock.opCodes
        for (i in opCodes.indices) {
            val end = i + windowSize
            if (end <= opCodes.size) {
                val window = opCodes.subList(i, end)
                val optimized = optimizeWindow(window)
                if (optimized != null) {
                    modified = true
                    window.clear()
                    window.addAll(optimized)
                }
            }
        }

        return modified
    }

    /**
     * Try to optimize a window of IR-sequence. If the optimizer detects
     * a pattern it can optimize, it will return a list of instructions
     * replacing the instructions of the original window. Otherwise it
     * will return null.
     */
    protected abstract fun optimizeWindow(window: List<IR>): List<IR>?
}

/**
 * Loading a variable by storing to same variable is a no-op: remove instructions.
 */
private object RedundantLoadStoreOptimizer : PeepholeOptimizer(2) {
    override fun optimizeWindow(window: List<IR>): List<IR>? {
        val (first, second) = window

        return if (first is IR.LoadLocal && second is IR.StoreLocal && first.index == second.index)
            listOf()
        else
            null
    }
}

/**
 * Storing a variable and then loading the same variable can be replaced by dup + store.
 */
private object RedundantLoadOptimizer : PeepholeOptimizer(2) {
    override fun optimizeWindow(window: List<IR>): List<IR>? {
        val (first, second) = window

        return if (first is IR.StoreLocal && second is IR.LoadLocal && first.index == second.index)
            listOf(IR.Dup, first)
        else
            null
    }
}

/**
 * Removes PushUnit + Pop -combinations
 */
private object RedundantPushUnitPopOptimizer : PeepholeOptimizer(2) {
    override fun optimizeWindow(window: List<IR>): List<IR>? {
        val (first, second) = window

        return if (first == IR.PushUnit && second == IR.Pop)
            listOf()
        else
            null
    }
}
