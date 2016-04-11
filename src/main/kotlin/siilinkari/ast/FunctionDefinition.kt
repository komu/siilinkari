package siilinkari.ast

import siilinkari.types.Type

data class FunctionDefinition(val name: String,
                              val args: List<Pair<String, Type>>,
                              val returnType: Type?,
                              val body: Expression)
