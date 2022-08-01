package eu.grigoriu.craftinginterpreters.klox

import java.util.*

class Resolver(private val interpreter: Interpreter, private val errorReporter: ErrorReporter) :
    Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes = Stack<MutableMap<String, Boolean>>()

    private var currentFunction = FunctionType.NONE

    fun resolve(statements: List<Stmt?>) {
        for (statement in statements.filterNotNull()) {
            resolve(statement)
        }
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        declare(stmt.name)
        define(stmt.name)

        beginScope()
        scopes.peek()["this"] = true

        for (method in stmt.methods) {
            val declaration = FunctionType.METHOD
            resolveFunction(method, declaration)
        }

        endScope()
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.empty() && scopes.peek()[expr.name.lexeme] == false) {
            errorReporter.error(expr.name, "Can't read local variable in its own initializer.")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)

        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expression)
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch)
        }
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expression)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            errorReporter.error(stmt.keyword, "Can't return from top-level code.")
        }

        if (stmt.value != null) {
            resolve(stmt.value)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitThisExpr(expr: Expr.This) {
        resolveLocal(expr, expr.keyword)
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.right)
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun declare(name: Token) {
        if (scopes.empty()) {
            return
        }

        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            errorReporter.error(name, "Already a variable with this name in this scope.")
        }

        // Variable not ready yet.
        scope[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.empty()) {
            return
        }
        // Variable ready.
        scopes.peek()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in (scopes.size - 1) downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
            }
        }
    }
}

private enum class FunctionType {
    NONE,
    FUNCTION,
    METHOD,
}
