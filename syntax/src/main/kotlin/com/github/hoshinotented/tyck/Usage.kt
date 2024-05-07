package com.github.hoshinotented.tyck

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.FreeRefTerm
import com.github.hoshinotented.syntax.core.Term
import com.github.hoshinotented.syntax.core.Term.Companion.forEach
import kala.value.primitive.MutableIntValue

data class Usage(val ref: FreeBinding) {
  fun find(term: Term): Int {
    return when (term) {
      is FreeRefTerm -> if (term.ref === ref) 1 else 0
      else -> {
        val acc = MutableIntValue.create()
        
        term.forEach { t ->
          acc.add(find(t))
        }
        
        acc.get()
      }
    }
  }
}