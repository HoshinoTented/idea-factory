package com.github.hoshinotented.tyck

import com.github.hoshinotented.tyck.ctx.LocalContext

interface Contextful<This : Contextful<This>> {
  val localCtx: LocalContext
  
  fun setLocalCtx(newCtx: LocalContext): This
  
  fun <R> subscoped(block: This.() -> R): R {
    return setLocalCtx(localCtx.derive()).block()
  }
}