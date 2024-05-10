package com.github.hoshinotented.syntax.core

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.tyck.Usage
import com.github.hoshinotented.util.Nested
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
  
  fun bindAll(binds: SeqView<FreeBinding>): Term {
    return binds.foldLeftIndexed(this) { idx, acc, bind ->
      acc.bindAt(bind, idx)
    }
  }
  
  fun bindTele(tele: SeqView<FreeBinding>): Term = bindAll(tele.reversed())
  
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
  companion object {
    fun make(f: Term, a: Term): Term {
      return make(AppTerm(f, a))
    }
    
    fun make(t: AppTerm): Term {
      return when (t.f) {
        is LamTerm -> t.f.body.instantiate(t.a)
        else -> t
      }
    }
  }
  
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

/**
 * What LetTerm should do:
 * * Laziness: [definedAs] should be evaluated once
 * * Equivalence: the reference to [name] should judgemental equal to [definedAs]
 *
 * ## Solution
 *
 * When we may use [LetTerm], we need a [com.github.hoshinotented.tyck.LocalDeful],
 * if we want to hand off a [Term] to someone that is not a [com.github.hoshinotented.tyck.LocalDeful],
 * we need to may it a [LetTerm].
 */
data class LetTerm(override val param: Bind, override val body: Term) : Term, Nested<LetTerm.Bind, Term, LetTerm> {
  companion object {
    fun make(bind: FreeBinding, type: Term, definedAs: Term, body: Term): Term {
      return if (Usage(bind).find(body) > 0) {
        return LetTerm(Bind(Term.Param(bind.name, type), definedAs), body.bind(bind))
      } else body
    }
  }
  
  data class Bind(val name: Term.Param, val definedAs: Term) {
    fun map(mapper: (Int, Term) -> Term): Bind {
      return copy(name = name.map(mapper), definedAs = mapper(0, definedAs))
    }
    
    override fun toString(): String {
      return "| $name := $definedAs"
    }
  }
  
  override val bodyMaybe: LetTerm? = body as? LetTerm
  override fun safeCast(term: LetTerm): Term {
    return term
  }
  
  override fun map(f: (Int, Term) -> Term): Term {
    return copy(param = param.map(f), body = f(1, body))
  }
  
  override fun toString(): String {
    return "let $param in $body"
  }
}