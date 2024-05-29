package com.github.hoshinotented.tyck

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.FreeRefTerm
import com.github.hoshinotented.syntax.core.Term
import com.github.hoshinotented.syntax.core.Term.Companion.forEach
import kala.collection.immutable.ImmutableMap
import kala.collection.mutable.MutableMap
import kala.value.primitive.MutableIntValue

data class Usage(val ref: FreeBinding) {
  companion object {
    private fun findAll(acc: MutableMap<FreeBinding, Int>, term: Term) {
      when (term) {
        is FreeRefTerm -> {
          val old = acc.getOrDefault(term.ref, 0)
          acc.put(term.ref, old + 1)
        }
        
        else -> {
          term.forEach { t ->
            findAll(acc, t)
          }
        }
      }
    }
    
    fun findAll(term: Term): ImmutableMap<FreeBinding, Int> {
      val acc = MutableMap.create<FreeBinding, Int>()
      findAll(acc, term)
      return acc.toImmutableMap()
    }
  }
  
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