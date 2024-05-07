package com.github.hoshinotented.syntax.concrete

import com.github.hoshinotented.resolve.Binding
import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.concrete.Expr.Let.Bind
import com.github.hoshinotented.util.Nested
import kala.collection.immutable.ImmutableSeq

sealed interface Expr {
  fun map(mapper: (Expr) -> Expr): Expr
  
  sealed interface PrExpr : Expr
  
  data object Type : Expr {
    override fun map(mapper: (Expr) -> Expr): Expr {
      return this
    }
  }
  
  data object BoolTy : Expr {
    override fun map(mapper: (Expr) -> Expr): Expr {
      return this
    }
  }
  
  data class Bool(val value: Boolean) : Expr {
    override fun map(mapper: (Expr) -> Expr): Expr {
      return this
    }
  }
  
  data class Param(val name: FreeBinding, val type: Expr) {
    fun map(f: (Expr) -> Expr): Param {
      return copy(type = f(type))
    }
  }
  
  data class App(val f: Expr, val a: Expr) : Expr {
    override fun map(mapper: (Expr) -> Expr): Expr {
      return App(mapper(f), mapper(a))
    }
  }
  
  data class Pi(override val param: Param, val last: Expr) : Expr, Nested<Param, Expr, Pi> {
    override fun safeCast(term: Pi): Expr {
      return term
    }
    
    override val body: Expr = last
    override val bodyMaybe: Pi? = last as? Pi
    
    override fun map(mapper: (Expr) -> Expr): Expr {
      return copy(param = param.map(mapper), last = mapper(last))
    }
  }
  
  data class Sigma(val param: ImmutableSeq<Param>, val last: Expr) : Expr {
    override fun map(mapper: (Expr) -> Expr): Expr {
      return copy(param = param.map { it.map(mapper) }, last = mapper(last))
    }
  }
  
  data class RawRef(val name: String) : PrExpr {
    override fun map(mapper: (Expr) -> Expr): Expr {
      return this
    }
  }
  
  data class Ref(val name: Binding) : Expr {
    override fun map(mapper: (Expr) -> Expr): Expr {
      return this
    }
  }
  
  data class Lam(override val param: FreeBinding, override val body: Expr) : Expr, Nested<FreeBinding, Expr, Lam> {
    override val bodyMaybe: Lam? = body as? Lam
    
    override fun safeCast(term: Lam): Expr {
      return term
    }
    
    override fun map(mapper: (Expr) -> Expr): Expr {
      return copy(body = mapper(body))
    }
  }
  
  data class Tup(val elements: ImmutableSeq<Expr>) : Expr {
    override fun map(mapper: (Expr) -> Expr): Expr {
      return Tup(elements.map(mapper))
    }
  }
  
  data class Let(override val param: Bind, override val body: Expr) : Expr, Nested<Bind, Expr, Let> {
    data class Bind(val bind: FreeBinding, val definedAs: Expr)
    
    override fun map(mapper: (Expr) -> Expr): Expr {
      return copy(param = param.copy(definedAs = mapper(param.definedAs)), body = mapper(body))
    }
    
    override val bodyMaybe: Let? = body as? Let
    
    override fun safeCast(term: Let): Expr {
      return term
    }
  }
}