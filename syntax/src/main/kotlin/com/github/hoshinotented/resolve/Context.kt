package com.github.hoshinotented.resolve

sealed interface ContextElement {
  val asRef: Binding
  
  data class Local(val binding: FreeBinding) : ContextElement {
    override val asRef: Binding = binding
  }
}

/**
 * A [Context] for [ExprResolver]
 */
interface Context {
  /** @return parent [Context], null if this one is top level. */
  val parent: Context?
  
  /**
   * Resolve [name] to some [ContextElement]
   * @param name the name
   * @return a [ContextElement] correspond to [name], null if failed to resolve
   */
  operator fun get(name: String): ContextElement?
  
  /**
   * Empty [Context], used for root context.
   */
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