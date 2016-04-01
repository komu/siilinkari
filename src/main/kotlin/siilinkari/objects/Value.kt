package siilinkari.objects

val String.value: Value.String
    get() = Value.String(this)

val Int.value: Value.Integer
    get() = Value.Integer(this)

val Boolean.value: Value.Bool
    get() = Value.Bool(this)

sealed class Value {

    class String(val value: kotlin.String) : Value()  {
        override fun equals(other: Any?) = other is String && value == other.value
        override fun hashCode() = value.hashCode()
        override fun toString() = '"' + value.replace("\"", "\\\"") + '"'
    }

    sealed class Bool(val value: Boolean) : Value() {
        object True : Bool(true)
        object False : Bool(false)

        companion object {
            operator fun invoke(value: Boolean): Bool = if (value) True else False
        }

        override fun equals(other: Any?) = other is Bool && value == other.value
        override fun hashCode() = value.hashCode()
        override fun toString() = value.toString()
        operator fun not() = if (this == True) False else True
    }

    class Integer(val value: Int) : Value()  {
        override fun equals(other: Any?) = other is Integer && value == other.value
        override fun hashCode() = value.hashCode()
        override fun toString() = value.toString()

        operator fun plus(other: Integer) = Integer(value + other.value)
        operator fun minus(other: Integer) = Integer(value - other.value)
    }
}
