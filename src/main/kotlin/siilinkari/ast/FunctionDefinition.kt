package siilinkari.ast

import siilinkari.types.Type

data class FunctionDefinition(val name: String,
                              val args: List<Pair<String, Type>>,
                              val returnType: Type,
                              val body: Expression) {
    val signature = Type.Function(args.map { it.second }, returnType)
    val argumentCount: Int = args.size
}
