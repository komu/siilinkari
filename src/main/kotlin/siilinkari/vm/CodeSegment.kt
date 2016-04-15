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

    val size: Int
        get() = opCodes.size

    fun mergeWithRelocatedSegment(segment: CodeSegment): Pair<CodeSegment, Int> {
        val ops = ArrayList<OpCode>(opCodes.size + segment.opCodes.size)
        val address = opCodes.size

        ops += opCodes
        for (op in segment.opCodes)
            ops += op.relocate(address)

        return Pair(CodeSegment(ops), address)
    }

    /**
     * Extracts given region of code, relocated so that every address makes sense
     * (assuming the addresses stay within the region).
     *
     * Useful for dumping function code for inspection.
     */
    fun getRegion(address: Int, size: Int): CodeSegment {
        val ops = opCodes.subList(address, address + size)

        return CodeSegment(ops.map { it.relocate(-address) })
    }
}
