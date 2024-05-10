package com.github.hoshinotented.tyck.ctx

import kala.collection.mutable.MutableMap
import kala.control.Option

interface Scoped<K : Any, V : Any, This : Scoped<K, V, This>> {
  companion object {
    // fold from bottom
    inline fun <K, V, This, U> This.fold(zero: U, folder: (U, This) -> U): U
            where K : Any,
                  V : Any,
                  This : Scoped<K, V, This> {
      var ctx: This? = this
      var acc = zero
      while (ctx != null) {
        acc = folder(acc, ctx)
        ctx = ctx.parent
      }
      
      return acc
    }
  }
  
  val parent: This?
  val map: MutableMap<K, V>
  val self: This
  
  fun derive(): This
  
  fun containsLocal(key: K): Boolean {
    return map.containsKey(key)
  }
  
  fun getLocal(key: K): V? {
    return map.getOrNull(key)
  }
  
  operator fun contains(key: K): Boolean {
    return self.fold(false) { acc, ctx ->
      acc || ctx.containsLocal(key)
    }
  }
  
  fun getOrNull(key: K): V? {
    return self.fold(Option.none<V>()) { acc, ctx ->
      acc.orElse {
        Option.ofNullable(ctx.getLocal(key))
      }
    }.orNull
  }
  
  operator fun get(key: K): V {
    return getOrNull(key) ?: throw IllegalStateException("Not in scope: $key")
  }
  
  operator fun set(key: K, value: V) {
    if (key in this) {
      throw IllegalStateException("Exist $key: $value")
    }
    
    map[key] = value
  }
}