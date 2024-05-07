package com.github.hoshinotented.syntax.core

import com.github.hoshinotented.resolve.FreeBinding
import kala.collection.SeqView

sealed interface Term {
  companion object {
    fun Term.map(f: (Term) -> Term): Term = map { _, t ->
      f(t)
    }
    
    fun Term.forEach(f: (Term) -> Unit): Unit {
      map { t -> f(t); t }
    }
    
    fun Term.instantiate(bind: FreeBinding): Term = instantiate(FreeRefTerm(bind))
  }
  
  fun bindAt(ref: FreeBinding, index: Int): Term = map { idx, t ->
    t.bindAt(ref, index + idx)
  }
  
  fun bind(ref: FreeBinding) = bindAt(ref, 0)
  
  fun replace(i: Int, term: Term): Term = map { idx, t ->
    t.replace(i + idx, term)
  }
  
  fun instantiate(term: Term) = replace(0, term)
  
  fun instantiateAll(terms: SeqView<Term>) {
    terms.foldLeftIndexed(this) { idx, acc, t ->
      acc.replace(idx, t)
    }
  }
  
  fun instantiateTele(tele: SeqView<Term>) = instantiateAll(tele.reversed())
  
  fun map(f: (Int, Term) -> Term): Term
  
  data class Param(val name: String, val type: Term) {
    fun map(f: (Int, Term) -> Term): Param {
      return copy(type = f(0, type))
    }
    
    override fun toString(): String {
      return "($name : $type)"
    }
  }
}

sealed interface LeafTerm : Term {
  override fun map(f: (Int, Term) -> Term): Term {
    return this
  }
}

sealed interface StableWHNF : Term

data class FreeParam(val binding: FreeBinding, val type: Term) {
  fun forget(): Term.Param {
    return Term.Param(binding.name, type)
  }
}

data class FreeRefTerm(val ref: FreeBinding) : LeafTerm {
  override fun bindAt(ref: FreeBinding, index: Int): Term {
    return if (this.ref == ref) {
      BoundRefTerm(index)
    } else this
  }
  
  override fun toString(): String {
    return ref.name
  }
}

data class BoundRefTerm(val index: Int) : LeafTerm {
  init {
    assert(index >= 0)
  }
  
  override fun replace(i: Int, term: Term): Term {
    return if (this.index == i) {
      term
    } else this
  }
  
  override fun toString(): String {
    return "^$index"
  }
}

data class LamTerm(val body: Term) : Term, StableWHNF {
  override fun map(f: (Int, Term) -> Term): Term {
    return copy(body = f(1, body))
  }
  
  override fun toString(): String {
    return "λ. $body"
  }
}

data class AppTerm(val f: Term, val a: Term) : Term {
  override fun map(f: (Int, Term) -> Term): Term {
    return AppTerm(f(0, this.f), f(0, a))
  }
  
  override fun toString(): String {
    return "$f ($a)"
  }
}

data class BoolTerm(val value: Boolean) : Term, StableWHNF, LeafTerm {
  override fun toString(): String {
    return value.toString()
  }
}

data class LetTerm(val name: Term.Param, val definedAs: Term, val body: Term) : Term {
  override fun map(f: (Int, Term) -> Term): Term {
    return copy(name = name.map(f), definedAs = f(0, definedAs), body = f(1, body))
  }
  
  override fun toString(): String {
    return "let ${name.name} : ${name.type} := $definedAs in $body"
  }
}