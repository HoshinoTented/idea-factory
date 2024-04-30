package com.github.hoshinotented.resolve

sealed interface ContextElement {
  val asRef: Binding
  
  data class Local(val binding: FreeBinding) : ContextElement {
    override val asRef: Binding = binding
  }
}

interface Context {
  val parent: Context?
  operator fun get(name: String): ContextElement?
  
  data object Empty : Context {
    override val parent: Context? = null
    override fun get(name: String): ContextElement? {
      return null
    }
  }
  
  data class Bind(override val parent: Context, val bind: ContextElement.Local) : Context {
    override fun get(name: String): ContextElement? {
      if (bind.binding.name == name) return bind
      return parent[name]
    }
  }
}