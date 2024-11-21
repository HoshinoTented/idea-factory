package org.aya.tool.classfile

import kala.collection.SeqView
import java.lang.reflect.AccessFlag

@JvmInline
value class AccessFlagBuilder(val flags: SeqView<AccessFlag>) {
  fun static(): AccessFlagBuilder {
    return AccessFlagBuilder(flags.appended(AccessFlag.STATIC))
  }
  
  fun final(): AccessFlagBuilder {
    return AccessFlagBuilder(flags.appended(AccessFlag.FINAL))
  }
  
  fun trait(): AccessFlagBuilder {
    return AccessFlagBuilder(flags.appended(AccessFlag.INTERFACE))
  }
  
  fun mask(): Int {
    return flags.fold(0) { acc, flag ->
      acc or flag.mask()
    }
  }
}

fun public(): AccessFlagBuilder {
  return AccessFlagBuilder(SeqView.of(AccessFlag.PUBLIC))
}