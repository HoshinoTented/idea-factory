import com.github.hoshinotented.resolve.Context
import com.github.hoshinotented.resolve.ExprResolver
import com.github.hoshinotented.syntax.concrete.Expr
import com.github.hoshinotented.syntax.core.Term
import com.github.hoshinotented.tyck.ExprTycker
import com.github.hoshinotented.tyck.ctx.LocalContext
import kala.collection.mutable.MutableMap

fun resolve(code: Expr): Expr {
  return ExprResolver(Context.Empty).invoke(code)
}

fun tyck(code: Expr, type: Expr): Term {
  val wellTy = ExprTycker(LocalContext(null, MutableMap.create())).ty(type).wellTyped
  val wellTyped = ExprTycker(LocalContext(null, MutableMap.create())).inherit(code, wellTy).wellTyped
  return wellTyped
}

fun check(expr: Expr, type: Expr): Term {
  val expr = resolve(expr)
  val type = resolve(type)
  return tyck(expr, type)
}