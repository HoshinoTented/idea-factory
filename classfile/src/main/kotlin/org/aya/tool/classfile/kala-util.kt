package org.aya.tool.classfile

import kala.collection.SeqView

fun <T> SeqView(iter: MutableIterable<T>): SeqView<T> = object : SeqView<T> {
  override fun iterator(): MutableIterator<T> {
    return iter.iterator()
  }
}