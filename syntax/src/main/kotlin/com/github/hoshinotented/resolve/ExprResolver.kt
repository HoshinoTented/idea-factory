package com.github.hoshinotented.resolve

import com.github.hoshinotented.syntax.concrete.Expr

data class ExprResolver(val ctx: Context) : (Expr) -> Expr {
  companion object {
    fun Context.bind(bind: FreeBinding): Context {
      return Context.Bind(this, ContextElement.Local(bind))
    }
    
    fun Context.enter(): ExprResolver {
      return ExprResolver(this)
    }
  }
  
  override fun invoke(expr: Expr): Expr {
    return when (expr) {
      is Expr.Pi -> expr.map(ctx.bind(expr.param.name).enter())
      is Expr.Lam -> expr.map(ctx.bind(expr.param).enter())
      is Expr.RawRef -> Expr.Ref(
        ctx[expr.name]?.asRef
          ?: throw IllegalStateException("Unresolved symbol: ${expr.name}")
      )
      
      is Expr.Ref -> expr
      is Expr.Sigma -> TODO()
      is Expr.Tup -> TODO()
      is Expr.App -> expr.map(this)
      is Expr.Bool -> expr
      Expr.BoolTy -> expr
      Expr.Type -> expr
    }
  }
  
}