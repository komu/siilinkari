package siilinkari.types

import siilinkari.objects.Value

sealed class TypedExpression(val type: Type) {
    class Ref(val name: String, type: Type) : TypedExpression(type)
    class Lit(val value: Value, type: Type) : TypedExpression(type)
    class Not(val exp: TypedExpression): TypedExpression(Type.Boolean)
    sealed class Binary(val lhs: TypedExpression, val rhs: TypedExpression, type: Type): TypedExpression(type) {
        class Plus(lhs: TypedExpression, rhs: TypedExpression, type: Type) : Binary(lhs, rhs, type)
        class Minus(lhs: TypedExpression, rhs: TypedExpression, type: Type) : Binary(lhs, rhs, type)
        class Equals(lhs: TypedExpression, rhs: TypedExpression) : Binary(lhs, rhs, Type.Boolean)
        class NotEquals(lhs: TypedExpression, rhs: TypedExpression) : Binary(lhs, rhs, Type.Boolean)
    }
}
