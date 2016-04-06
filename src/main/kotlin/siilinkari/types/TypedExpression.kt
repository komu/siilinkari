package siilinkari.types

import siilinkari.ast.RelationalOp
import siilinkari.env.Binding
import siilinkari.objects.Value

/**
 * Represents an expression that has been type-checked and therefore has a known [type].
 *
 * The tree is mostly analogous to expressions in the original AST, but there are some
 * differences.
 *
 * For example, there are some new nodes with more explicit meaning. For example, while `1 + 2` is translated
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

        /** Numeric multiplication */
        class Multiply(lhs: TypedExpression, rhs: TypedExpression, type: Type) : Binary(lhs, rhs, type)

        /** Numeric division */
        class Divide(lhs: TypedExpression, rhs: TypedExpression, type: Type) : Binary(lhs, rhs, type)

        /** String concatenation */
        class ConcatString(lhs: TypedExpression, rhs: TypedExpression) : Binary(lhs, rhs, Type.String)

        /** =, !=, <, >, <=, >= */
        class Relational(val op: RelationalOp, lhs: TypedExpression, rhs: TypedExpression) : Binary(lhs, rhs, Type.Boolean)
    }
}
