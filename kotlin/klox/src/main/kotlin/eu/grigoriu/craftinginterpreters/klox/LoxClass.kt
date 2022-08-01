package eu.grigoriu.craftinginterpreters.klox

class LoxClass(val name: String, private val methods: MutableMap<String, LoxFunction>) : LoxCallable {
    override val arity: Int
        get() = 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        return LoxInstance(this)
    }

    override fun toString(): String {
        return name
    }

    fun findMethod(name: String): LoxFunction? {
        return methods[name]
    }
}
