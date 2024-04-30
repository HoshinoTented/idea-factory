package com.github.hoshinotented.util

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.FreeRefTerm
import com.github.hoshinotented.syntax.core.Term
import kala.collection.mutable.MutableMap

/**
 * Order Independent Substitution,
 * if one substitution contains another variable that would be substituted by this,
 * the behavior is undefined.
 */
data class Substitution(val map: MutableMap<FreeBinding, Term>) {
  companion object {
    fun Term.subst(subst: Substitution): Term {
      return map { it ->
        if (it is FreeRefTerm) {
          subst.map.getOrDefault(it.ref, it)
        } else it
      }
    }
    
    fun Term.subst(name: FreeBinding, term: Term): Term {
      return subst(Substitution(MutableMap.of(name, term)))
    }
  }
  
  operator fun set(name: FreeBinding, term: Term) {
    val exist = map.putIfAbsent(name, term)
    if (exist.isNotEmpty) throw IllegalArgumentException("exists: ${exist.get()}")
  }
}