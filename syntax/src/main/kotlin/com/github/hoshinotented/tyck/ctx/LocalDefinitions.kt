package com.github.hoshinotented.tyck.ctx

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.LetTerm
import com.github.hoshinotented.syntax.core.Term
import com.github.hoshinotented.tyck.ExprTycker
import kala.collection.SeqView
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableLinkedHashMap
import org.jetbrains.annotations.Contract

data class LocalDefinitions(
  override val parent: LocalDefinitions?,
  // the order is very very very very very very very important
  override val map: MutableLinkedHashMap<FreeBinding, ExprTycker.Result<Term>>,
) : Scoped<FreeBinding, ExprTycker.Result<Term>, LocalDefinitions> {
  companion object {
    fun create(): LocalDefinitions {
      return LocalDefinitions(null, MutableLinkedHashMap.of())
    }
  }
  
  override val self: LocalDefinitions = this
  
  override fun derive(): LocalDefinitions {
    return LocalDefinitions(this, MutableLinkedHashMap.from(map))
  }
  
  @Contract(mutates = "this")
  fun override(bind: FreeBinding, term: Term): Term {
    val old = map.get(bind)!!   // exception if no [bind]
    map.put(bind, ExprTycker.Result.Default(term, old.type))
    return old.wellTyped
  }
  
  fun asLetBinds(): ImmutableSeq<Pair<FreeBinding, LetTerm.Bind>> {
    fun asLetBindsView(ld: LocalDefinitions): SeqView<Pair<FreeBinding, LetTerm.Bind>> {
      val parent = if (ld.parent != null) asLetBindsView(ld.parent) else SeqView.empty()
      val self = ld.map.toImmutableSeq().view()
        .map {
          it.key to LetTerm.Bind(Term.Param(it.key.name, it.component2().type), it.component2().wellTyped)
        }
      
      return parent.concat(self)
    }
    
    return asLetBindsView(this).toImmutableSeq()
  }
}