package com.github.hoshinotented.tyck.ctx

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.tyck.ExprTycker
import kala.collection.mutable.MutableMap

data class LocalDefinitions(
  override val parent: LocalDefinitions?,
  override val map: MutableMap<FreeBinding, ExprTycker.Result<*>>,
) : Scoped<FreeBinding, ExprTycker.Result<*>, LocalDefinitions> {
  override val self: LocalDefinitions = this
  
  override fun derive(): LocalDefinitions {
    return LocalDefinitions(this, MutableMap.from(map))
  }
}