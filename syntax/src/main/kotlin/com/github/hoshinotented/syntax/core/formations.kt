package com.github.hoshinotented.syntax.core

import com.github.hoshinotented.resolve.FreeBinding

sealed interface Formation : Term

data class PiTerm(val param: Term.Param, val last: Term) : Term, Formation, StableWHNF {
  override fun map(f: (Int, Term) -> Term): Term {
    return copy(param = param.map(f), last = f(1, last))
  }
  
  override fun toString(): String {
    return "$param -> ${last.instantiate(FreeRefTerm(FreeBinding(param.name)))}"
  }
}

data object BoolTyTerm : Term, Formation, StableWHNF, LeafTerm

// Type in Type
data object Type : Term, LeafTerm, StableWHNF, Formation