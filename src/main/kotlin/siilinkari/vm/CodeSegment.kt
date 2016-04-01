package siilinkari.vm

import java.util.*

class CodeSegment {

    private val code = ArrayList<OpCode>()

    operator fun plusAssign(op: OpCode) {
        code += op
    }

    operator fun get(address: Int): OpCode = code[address]

    override fun toString(): String =
        code.mapIndexed { i, op -> "$i $op" }.joinToString("\n")

    val endAddress: Int
        get() = code.size
}
