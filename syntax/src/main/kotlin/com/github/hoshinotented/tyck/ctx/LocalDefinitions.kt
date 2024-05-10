package com.github.hoshinotented.tyck.ctx

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.Term
import com.github.hoshinotented.tyck.ExprTycker
import kala.collection.mutable.MutableMap
import org.jetbrains.annotations.Contract

data class LocalDefinitions(
  override val parent: LocalDefinitions?,
  override val map: MutableMap<FreeBinding, ExprTycker.Result<Term>>,
) : Scoped<FreeBinding, ExprTycker.Result<Term>, LocalDefinitions> {
  companion object {
    fun create(): LocalDefinitions {
      return LocalDefinitions(null, MutableMap.create())
    }
  }
  
  override val self: LocalDefinitions = this
  
  override fun derive(): LocalDefinitions {
    return LocalDefinitions(this, MutableMap.from(map))
  }
  
  @Contract(mutates = "this")
  fun override(bind: FreeBinding, term: Term): Term {
    val old = map.get(bind)!!   // exception if no [bind]
    map.put(bind, ExprTycker.Result.Default(term, old.type))
    return old.wellTyped
  }
}