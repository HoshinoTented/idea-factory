package com.github.hoshinotented.tyck

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.Term
import kala.collection.immutable.ImmutableMap

/**
 * [Term] with environment (local definitions)
 */
data class EnvTerm(val env: ImmutableMap<FreeBinding, EnvTerm>, val term: Term) {
}