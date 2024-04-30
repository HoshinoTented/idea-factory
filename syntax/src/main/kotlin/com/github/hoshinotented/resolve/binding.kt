package com.github.hoshinotented.resolve

sealed interface Binding {
}

data class FreeBinding(val name: String) : Binding {

}