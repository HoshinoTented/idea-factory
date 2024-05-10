package com.github.hoshinotented.util

import kala.collection.SeqView

interface Nested<Param, Term, This> where This : Nested<Param, Term, This> {
  val body: Term
  val bodyMaybe: This?
  val param: Param
  
  // term === safeCast(term)
  fun safeCast(term: This): Term
  
  companion object {
    fun <Param, Term, This : Nested<Param, Term, This>> build(
      params: SeqView<Param>,
      body: Term,
      ctor: (Param, Term) -> This
    ): This {
      return params.foldRight(body) { param, term ->
        val result = ctor(param, term)
        result.safeCast(result)
      } as This
    }
    
    inline fun <Param, Term, This> This.destruct(paramConsumer: (Param) -> Unit): Term
            where Param : Any,
                  Term : Any,
                  This : Nested<Param, Term, This> {
      var nested: This? = this
      var body: Term = this.body
      
      while (nested != null) {
        paramConsumer.invoke(nested.param)
        body = nested.body
        nested = nested.bodyMaybe
      }
      
      return body
    }
  }
}