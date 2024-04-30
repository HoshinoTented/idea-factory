package com.github.hoshinotented.tyck.ctx

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.FreeParam
import com.github.hoshinotented.syntax.core.Term
import kala.collection.mutable.MutableMap

data class LocalContext(val parent: LocalContext?, val map: MutableMap<FreeBinding, Term>) {
  operator fun get(bind: FreeBinding): Term {
    return map.getOrNull(bind) ?: parent?.get(bind) ?: throw IllegalStateException("Not in scope: ${bind.name}")
  }
  
  operator fun set(bind: FreeBinding, type: Term) {
    val exists = map.putIfAbsent(bind, type).getOrNull()
    if (exists != null) throw IllegalStateException("Existing binding (${bind.name} : $exists))")
  }
  
  fun put(param: FreeParam) {
    this[param.binding] = param.type
  }
  
  fun derive(): LocalContext {
    return LocalContext(this, MutableMap.create())
  }
}