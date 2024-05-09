package com.github.hoshinotented.tyck

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.concrete.Expr
import com.github.hoshinotented.syntax.core.*
import com.github.hoshinotented.tyck.WHNormalizer.whnf
import com.github.hoshinotented.tyck.ctx.LocalContext
import com.github.hoshinotented.tyck.ctx.LocalDefinitions

class ExprTycker(
  override val localCtx: LocalContext,
  override val localDefs: LocalDefinitions
) : Contextful<ExprTycker> {
  fun unifyReport(lhs: Term, rhs: Term, type: Term?) {
    // This is safe, since we can only modify LocalContexts those we construct
    val result = Conversion(localCtx, localDefs)
      .check(lhs, rhs, type)
    
    if (!result) {
      throw IllegalStateException("Unable to unify: |- $lhs = $rhs : $type")
    }
  }
  
  private fun badExpr(expr: Expr, type: Term): Nothing {
    throw IllegalStateException("Bad expr: |- $expr : $type")
  }
  
  /**
   * Check judgement like: [localCtx] |- [expr] : [type]
   */
  fun inherit(expr: Expr, type: Term): Result<Term> {
    val whTy = whnf(type)
    return when (expr) {
      is Expr.Lam -> if (whTy is PiTerm) {
        val wellBody = subscoped {
          localCtx[expr.param] = whTy.param.type
          inherit(expr.body, whTy.last.instantiate(FreeRefTerm(expr.param)))
        }
        
        Result.Default(LamTerm(wellBody.wellTyped.bind(expr.param)), type)
      } else badExpr(expr, type)
      
      is Expr.Let -> checkLet(expr) {
        inherit(it, type)
      }
      
      // fallback
      else -> {
        val result = synthesize(expr)
        unifyReport(result.type, type, null)
        return result
      }
    }
  }
  
  fun ty(expr: Expr): Result<Term> {
    return when (expr) {
      is Expr.Pi -> {
        val wellParam = param(expr.param)
        val wellLast = subscoped {
          localCtx.put(wellParam.wellTyped)
          synthesize(expr.last)
        }
        
        Result.Default(
          PiTerm(
            wellParam.wellTyped.forget(),
            wellLast.wellTyped.bind(wellParam.wellTyped.binding)
          ), Type
        )
      }
      
      is Expr.Sigma -> TODO()
      
      is Expr.Let -> checkLet(expr, this::ty)
      
      Expr.BoolTy -> Result.Default(BoolTyTerm, Type)
      Expr.Type -> Result.Default(Type, Type)
      
      else -> throw IllegalArgumentException("Not a type")
    }
  }
  
  fun synthesize(expr: Expr): Result<Term> {
    return when (expr) {
      is Expr.PrExpr -> throw IllegalArgumentException("Pre expr")
      
      is Expr.Ref -> when (expr.name) {
        is FreeBinding -> Result.Default(FreeRefTerm(expr.name), localCtx[expr.name])
      }
      
      is Expr.App -> {
        val f = synthesize(expr.f)
        val fTy = whnf(f.type)
        if (fTy !is PiTerm) throw IllegalStateException("Not a Pi: ${expr.f}")
        
        val a = inherit(expr.a, fTy.param.type)
        
        return Result.Default(AppTerm(f.wellTyped, a.wellTyped), fTy.last.instantiate(a.wellTyped))
      }
      
      is Expr.Pi -> ty(expr)
      is Expr.Lam -> noRule(expr)
      is Expr.Sigma -> TODO()
      is Expr.Tup -> TODO()
      
      is Expr.Let -> checkLet(expr, this::synthesize)
      
      is Expr.Bool -> Result.Default(BoolTerm(expr.value), BoolTyTerm)
      Expr.BoolTy, Expr.Type -> ty(expr)
    }
  }
  
  private fun checkLetBody(term: Term, bind: FreeBinding, bindTy: Term, definedAs: Term): Term {
    return if (Usage(bind).find(term) > 0) {
      LetTerm(Term.Param(bind.name, bindTy), definedAs, term.bind(bind))
    } else term.bind(bind)
  }
  
  private fun checkLet(expr: Expr.Let, checker: (Expr) -> Result<Term>): Result<Term> {
    val wellBind = checker(expr.param.definedAs)
    val wellBody = subscoped {
      localCtx[expr.param.bind] = wellBind.type
      checker(expr.body)
    }
    
    // Note that the type of wellBody may refer to [expr.param.bind]
    val wellBodyTm = checkLetBody(wellBody.wellTyped, expr.param.bind, wellBind.type, wellBind.wellTyped)
    val wellBodyTy = checkLetBody(wellBody.type, expr.param.bind, wellBind.type, wellBind.wellTyped)
    
    return Result.Default(wellBodyTm, wellBodyTy)
  }
  
  private fun param(param: Expr.Param): Result<FreeParam> {
    val wellTyped = synthesize(param.type)
    return Result.Default(FreeParam(param.name, wellTyped.wellTyped), wellTyped.wellTyped)
  }
  
  private fun noRule(expr: Expr): Nothing {
    throw IllegalStateException("No rule for $expr")
  }
  
  sealed interface Result<out T> {
    val wellTyped: T
    val type: Term
    
    data class Default<out T>(override val wellTyped: T, override val type: Term) : Result<T>
  }
  
  override fun set(newCtx: LocalContext, newDef: LocalDefinitions): ExprTycker {
    return ExprTycker(newCtx, newDef)
  }
}