package com.github.hoshinotented.tyck

import com.github.hoshinotented.syntax.core.Term
import com.github.hoshinotented.tyck.ctx.LocalContext
import com.github.hoshinotented.tyck.ctx.LocalDefinitions

abstract class AbstractTycker<This : AbstractTycker<This>>(
  override val localCtx: LocalContext,
  override val localDefs: LocalDefinitions
) : Contextful<This>, LocalDeful<This> {
  fun whnf(term: Term): Term {
    return WHNormalizer(localDefs).invoke(term)
  }
  
  override fun <R> subscoped(block: This.() -> R): R {
    return set(localCtx.derive(), localDefs.derive()).block()
  }
  
  override fun set(newOne: LocalDefinitions): This {
    return set(localCtx, newOne)
  }
  
  override fun set(newCtx: LocalContext): This {
    return set(newCtx, localDefs)
  }
  
  abstract fun set(newCtx: LocalContext, newDefs: LocalDefinitions): This
}