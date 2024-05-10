package com.github.hoshinotented.tyck

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.*
import com.github.hoshinotented.syntax.core.Term.Companion.instantiate
import com.github.hoshinotented.syntax.core.Term.Companion.map
import com.github.hoshinotented.tyck.ctx.LocalDefinitions
import kala.collection.mutable.MutableMap

data class WHNormalizer
@JvmOverloads
constructor(override val localDefs: LocalDefinitions = LocalDefinitions(null, MutableMap.create())) : (Term) -> Term,
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
          val subst = localDefs[ref]
          this(subst.wellTyped)     // TODO: override localDefs and add make a mark
        } else postTerm
      }
      
      is AppTerm -> {
        val f = postTerm.f
        val a = postTerm.a
        
        if (f is LamTerm) {
          invoke(f.body.instantiate(a))
        } else postTerm
      }
      
      is LetTerm -> {
        val bind = FreeBinding(postTerm.param.name.name)
        subscoped {
          // we dont normalize definedAs for now, see [FreeRefTerm]
          localDefs[bind] = ExprTycker.Result.Default(postTerm.param.definedAs, postTerm.param.name.type)
          val freeBody = this@subscoped(postTerm.body.instantiate(bind))
          val newDefinedAs = localDefs[bind]        // normalizer may normalize [definedAs] more
          
          LetTerm.make(bind, newDefinedAs.type, newDefinedAs.wellTyped, freeBody)
        }
      }
      
      is BoundRefTerm -> postTerm
    }
  }
  
  override fun set(newOne: LocalDefinitions): WHNormalizer {
    return WHNormalizer(newOne)
  }
}