package eu.grigoriu.craftinginterpreters.klox

class Return(val value: Any?) : RuntimeException(null, null, false, false)