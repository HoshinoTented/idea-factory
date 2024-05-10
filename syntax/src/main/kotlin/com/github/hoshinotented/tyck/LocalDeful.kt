package com.github.hoshinotented.tyck

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.LetTerm
import com.github.hoshinotented.syntax.core.Term
import com.github.hoshinotented.tyck.ctx.LocalDefinitions
import com.github.hoshinotented.util.Nested.Companion.destruct
import kala.collection.mutable.MutableList

interface LocalDeful<This : LocalDeful<This>> {
  val localDefs: LocalDefinitions
  
  fun set(newOne: LocalDefinitions): This
  
  fun <R> subscoped(block: This.() -> R): R {
    return set(localDefs.derive()).block()
  }
  
  fun <R> load(letTerm: LetTerm, block: This.(Term) -> R): R {
    return subscoped {
      val binds = MutableList.create<FreeBinding>()
      val freeBody = letTerm.destruct {
        val bind = FreeBinding(it.name.name)
        binds.append(bind)
        localDefs[bind] = ExprTycker.Result.Default(it.definedAs, it.name.type)
      }
      
      this@subscoped.block(freeBody.bindTele(binds.view()))
    }
  }
}