package com.github.hoshinotented.tyck

import com.github.hoshinotented.tyck.ctx.LocalContext
import com.github.hoshinotented.tyck.ctx.LocalDefinitions

interface Contextful<This : Contextful<This>> {
  val localCtx: LocalContext
  val localDefs: LocalDefinitions
  
  fun set(newCtx: LocalContext, newDef: LocalDefinitions): This
  
  fun <R> subscoped(block: This.() -> R): R {
    return set(localCtx.derive(), localDefs.derive()).block()
  }
}