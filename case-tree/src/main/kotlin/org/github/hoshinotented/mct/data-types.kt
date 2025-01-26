package org.github.hoshinotented.mct

import kala.collection.immutable.ImmutableSeq

interface Constructor {
  companion object {
    operator fun String.invoke(vararg telescope: DataType): Constructor {
      return object : Constructor {
        override val name: String = this@invoke
        override val telescope: ImmutableSeq<DataType> = ImmutableSeq.from(telescope)
      }
    }
  }
  
  val name: String
  val telescope: ImmutableSeq<DataType>
}

sealed interface DataType {
  data object Indestructible : DataType
  interface Destructible : DataType {
    val constructors: ImmutableSeq<Constructor>
  }
}

sealed interface Pattern {
  companion object {
    fun check(patterns: Array<out Any>): ImmutableSeq<Pattern> {
      return ImmutableSeq.from(patterns).map {
        when (it) {
          is String -> Bind(it)
          is Pattern -> it
          else -> TODO()
        }
      }
    }
    
    operator fun Constructor.invoke(vararg patterns: Any): Pattern {
      return Con(this, check(patterns))
    }
  }
  
  data class Bind(val name: String) : Pattern
  data class Con(val con: Constructor, val inner: ImmutableSeq<Pattern>) : Pattern
}

data class Parameter(val name: String, val type: DataType)

data class Matching(val telescope: ImmutableSeq<Parameter>, val clauses: ImmutableSeq<Clause>) {
  init {
    assert(telescope.isNotEmpty)
    assert(clauses.allMatch { telescope.size() == it.patterns.size() })
  }
  
  data class Clause(val id: Int, val patterns: ImmutableSeq<Pattern>)
  
  fun drop(): Matching? {
    if (telescope.size() == 1) return null
    val dropTele = telescope.drop(1)
    val dropClauses = clauses.map { Clause(it.id, it.patterns.drop(1)) }
    return Matching(dropTele, dropClauses)
  }
}