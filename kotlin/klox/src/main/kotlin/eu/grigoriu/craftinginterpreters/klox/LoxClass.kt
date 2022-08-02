package eu.grigoriu.craftinginterpreters.klox

class LoxClass(
    val name: String,
    private val superclass: LoxClass?,
    private val methods: MutableMap<String, LoxFunction>,
) : LoxCallable {
    override val arity: Int
        get() {
            val initializer = findMethod("init") ?: return 0
            return initializer.arity
        }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)

        return instance
    }

    override fun toString(): String {
        return name
    }

    fun findMethod(name: String): LoxFunction? {
        return methods[name]
    }
}
