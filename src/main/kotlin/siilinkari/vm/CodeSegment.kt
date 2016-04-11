package siilinkari.vm

import java.util.*

/**
 * A random-accessible segment of opcodes.
 */
class CodeSegment(private val opCodes: List<OpCode> = emptyList()) {

    /**
     * Returns [OpCode] with given address.
     */
    operator fun get(address: Int): OpCode = opCodes[address]

    override fun toString(): String =
        opCodes.asSequence().mapIndexed { i, op -> "$i $op" }.joinToString("\n")

    fun mergeWithRelocatedSegment(segment: CodeSegment): Pair<CodeSegment, Int> {
        val ops = ArrayList<OpCode>(opCodes.size + segment.opCodes.size)
        val address = opCodes.size

        ops += opCodes
        for (op in segment.opCodes)
            ops += op.relocate(address)

        return Pair(CodeSegment(ops), address)
    }
}
