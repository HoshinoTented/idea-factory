package com.github.hoshinotented.tyck

import com.github.hoshinotented.syntax.core.Term

interface Unifiable {
  fun mkUnifier(): Conversion
  
  fun unifyReport(lhs: Term, rhs: Term, type: Term?) {
    // This is safe, since we can only modify LocalContexts those we construct
    val result = mkUnifier()
      .check(lhs, rhs, type)
    
    if (!result) {
      throw IllegalStateException("Unable to unify: |- $lhs = $rhs : $type")
    }
  }
}