package siilinkari.types

import siilinkari.objects.Value

/**
 * Represents an expression that has been type-checked and therefore has a known [type].
 *
 * This is mostly analogous to expressions in the original AST, but some redundancies
 * have been removed. For example, we don't have `Binary.NotEquals(l, r)` because it can be
 * represented as `Not(Binary.Equals(l, r))`. The type-checker performs the simplification
 * so that the following stages (i.e. translator) have simpler language to work with.
 *
 * @see TypedStatement
 * @see Type
 */
sealed class TypedExpression(val type: Type) {

    class Ref(val name: String, type: Type) : TypedExpression(type)

    class Lit(val value: Value, type: Type) : TypedExpression(type)

    class Not(val exp: TypedExpression): TypedExpression(Type.Boolean)

    sealed class Binary(val lhs: TypedExpression, val rhs: TypedExpression, type: Type): TypedExpression(type) {
        class Plus(lhs: TypedExpression, rhs: TypedExpression, type: Type) : Binary(lhs, rhs, type)
        class Minus(lhs: TypedExpression, rhs: TypedExpression, type: Type) : Binary(lhs, rhs, type)
        class Equals(lhs: TypedExpression, rhs: TypedExpression) : Binary(lhs, rhs, Type.Boolean)
    }
}
