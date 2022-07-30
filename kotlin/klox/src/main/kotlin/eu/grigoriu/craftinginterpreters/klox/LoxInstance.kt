package eu.grigoriu.craftinginterpreters.klox

class LoxInstance(private val klass: LoxClass) {
    private val fields = mutableMapOf<String, Any?>()

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }

        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    override fun toString(): String {
        return "${klass.name} instance"
    }
}
