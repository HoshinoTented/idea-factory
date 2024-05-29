package com.github.hoshinotented.tyck

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.*
import com.github.hoshinotented.syntax.core.Term.Companion.instantiate
import com.github.hoshinotented.syntax.core.Term.Companion.map
import com.github.hoshinotented.tyck.ctx.LocalDefinitions

data class WHNormalizer
@JvmOverloads
constructor(override val localDefs: LocalDefinitions = LocalDefinitions.create()) : (Term) -> Term,
  LocalDeful<WHNormalizer> {
  
  fun whnf(term: Term): Term {
    return this(term)
  }
  
  override fun invoke(term: Term): Term {
    if (term is StableWHNF) return term
    val postTerm = term.map(this)
    
    return when (postTerm) {
      is StableWHNF -> throw IllegalStateException("unreachable")
      is FreeRefTerm -> {
        val ref = postTerm.ref
        if (localDefs.contains(ref)) {
          this(localDefs[ref].wellTyped)
        } else postTerm
      }
      
      is AppTerm -> {
        val app = AppTerm.make(postTerm)
        
        if (app !== postTerm) {
          this(app)
        } else app
      }
      
      is LetTerm -> {
        val bind = FreeBinding(postTerm.param.name.name)
        subscoped {
          // we dont normalize definedAs for now, see [FreeRefTerm]
          localDefs[bind] = ExprTycker.Result.Default(postTerm.param.definedAs, postTerm.param.name.type)
          val freeBody = this@subscoped(postTerm.body.instantiate(bind))
          
          LetTerm.make(bind, postTerm.param.name.type, postTerm.param.definedAs, freeBody)
        }
      }
      
      is BoundRefTerm -> postTerm
    }
  }
  
  override fun set(newOne: LocalDefinitions): WHNormalizer {
    return WHNormalizer(newOne)
  }
}