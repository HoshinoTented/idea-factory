package com.github.hoshinotented.tyck.ctx

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.FreeParam
import com.github.hoshinotented.syntax.core.Term
import kala.collection.mutable.MutableMap

data class LocalContext(
  override val parent: LocalContext?,
  override val map: MutableMap<FreeBinding, Term>,
) : Scoped<FreeBinding, Term, LocalContext> {
  override val self: LocalContext = this
  
  fun put(param: FreeParam) {
    this[param.binding] = param.type
  }
  
  override fun derive(): LocalContext {
    return LocalContext(this, MutableMap.create())
  }
}