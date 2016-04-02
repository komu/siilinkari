package siilinkari.types

import siilinkari.env.Binding
import siilinkari.objects.Value

/**
 * Represents an expression that has been type-checked and therefore has a known [type].
 *
 * The tree is mostly analogous to expressions in the original AST, but there are some
 * differences.
 *
 * First of all, some redundancies have been removed. For example, we don't have `Binary.NotEquals(l, r)`
 * because it can be represented as `Not(Binary.Equals(l, r))`. The type-checker performs the simplification
 * so that the following stages (i.e. translator) have simpler language to work with.
 *
 * Second, there are some new nodes with more explicit meaning. For example, while `1 + 2` is translated
 * to [TypedExpression.Binary.Plus], expressions `"foo" + "bar"` or `"foo" + 1` will be translated
 * to [TypedExpression.Binary.ConcatString].
 *
 * @see TypedStatement
 * @see Type
 */
sealed class TypedExpression(val type: Type) {

    /** Reference to a variable. */
    class Ref(val binding: Binding) : TypedExpression(binding.type)

    /** Literal value */
    class Lit(val value: Value, type: Type) : TypedExpression(type)

    /** Logical not. */
    class Not(val exp: TypedExpression): TypedExpression(Type.Boolean)

    /** Function call. */
    class Call(val func: TypedExpression, val args: List<TypedExpression>, type: Type) : TypedExpression(type)

    /** Binary operators. */
    sealed class Binary(val lhs: TypedExpression, val rhs: TypedExpression, type: Type): TypedExpression(type) {

        /** Numeric addition */
        class Plus(lhs: TypedExpression, rhs: TypedExpression, type: Type) : Binary(lhs, rhs, type)

        /** Numeric subtraction */
        class Minus(lhs: TypedExpression, rhs: TypedExpression, type: Type) : Binary(lhs, rhs, type)

        /** Equality comparison */
        class Equals(lhs: TypedExpression, rhs: TypedExpression) : Binary(lhs, rhs, Type.Boolean)

        /** String concatenation */
        class ConcatString(lhs: TypedExpression, rhs: TypedExpression) : Binary(lhs, rhs, Type.String)
    }
}
