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
 * @see Stmt
 * @see Type
 */
sealed class TypedExpression(val type: Type) {

    /** Reference to a variable. */
    class Ref(val binding: Binding) : TypedExpression(binding.type) {
        override fun toString() = "[Ref ${binding.name}]"
    }

    /** Literal value */
    class Lit(val value: Value, type: Type) : TypedExpression(type) {
        constructor(value: Value.Integer): this(value, Type.Int)
        constructor(value: Value.Bool): this(value, Type.Boolean)
        constructor(value: Value.String): this(value, Type.String)
        override fun toString() = "[Lit ${value.repr()}]"
    }

    /** Logical not. */
    class Not(val exp: TypedExpression): TypedExpression(Type.Boolean) {
        override fun toString() = "[Not $exp]"
    }

    /** Function call. */
    class Call(val func: TypedExpression, val args: List<TypedExpression>, type: Type) : TypedExpression(type) {
        override fun toString() = "[Call $func $args]"
    }

    /** Binary operators. */
    sealed class Binary(val lhs: TypedExpression, val rhs: TypedExpression, type: Type): TypedExpression(type) {
        override fun toString() = "[${javaClass.simpleName} $lhs $rhs]"

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
        class Relational(val op: RelationalOp, lhs: TypedExpression, rhs: TypedExpression) : Binary(lhs, rhs, Type.Boolean) {
            override fun toString() = "[$op $lhs $rhs]"
        }
    }

    class Assign(val variable: Binding, val expression: TypedExpression) : TypedExpression(Type.Unit) {
        override fun toString() = "[Assign $variable $expression]"
    }

    class Var(val variable: Binding, val expression: TypedExpression) : TypedExpression(Type.Unit) {
        override fun toString() = "[Var $variable $expression]"
    }

    class If(val condition: TypedExpression, val consequent: TypedExpression, val alternative: TypedExpression?, type: Type) : TypedExpression(type) {
        init {
            require(alternative != null || type == Type.Unit)
        }

        override fun toString() = "[If $condition $consequent ${alternative ?: "[]"}]"
    }

    class While(val condition: TypedExpression, val body: TypedExpression) : TypedExpression(Type.Unit) {
        override fun toString() = "[While $condition $body]"
    }

    class ExpressionList(val expressions: List<TypedExpression>) : TypedExpression(Type.Unit) {
        override fun toString() = "[ExpressionList $expressions]"
    }
}
