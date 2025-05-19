package org.aya.tool.classfile

import kala.collection.immutable.ImmutableSet
import kala.collection.mutable.MutableEnumSet
import kala.collection.mutable.MutableSet
import java.lang.reflect.AccessFlag

@JvmInline
value class AccessFlagBuilder(val flags: MutableSet<AccessFlag> = MutableEnumSet.create(AccessFlag::class.java)) {
  fun static(): AccessFlagBuilder {
    return apply {
      flags.add(AccessFlag.STATIC)
    }
  }
  
  fun final(): AccessFlagBuilder = apply {
    flags.add(AccessFlag.FINAL)
  }
  
  fun trait(): AccessFlagBuilder = apply {
    flags.add(AccessFlag.INTERFACE)
  }
  
  fun synthetic(): AccessFlagBuilder = apply {
    flags.add(AccessFlag.SYNTHETIC)
  }
  
  fun build(): AccessFlagSet {
    return AccessFlagSet(flags.toSet())
  }
  
  @Deprecated("use flag", replaceWith = ReplaceWith("flags"))
  fun mask(): Int {
    return flags.fold(0) { acc, flag ->
      acc or flag.mask()
    }
  }
}

fun public(): AccessFlagBuilder {
  return AccessFlagBuilder().apply {
    flags.add(AccessFlag.PUBLIC)
  }
}

fun private(): AccessFlagBuilder {
  return AccessFlagBuilder().apply {
    flags.add(AccessFlag.PRIVATE)
  }
}

data class AccessFlagSet(private val set: ImmutableSet<AccessFlag>) {
  fun has(flag: AccessFlag): Boolean {
    return set.contains(flag)
  }
  
  operator fun plus(flag: AccessFlag): AccessFlagSet {
    return AccessFlagSet(set.added(flag))
  }
  
  fun toArray(): Array<AccessFlag> {
    return set.toArray(AccessFlag::class.java)
  }
  
  /**
   * @see jdk.internal.classfile.impl.Util.flagsToBits
   */
  fun mask(location: AccessFlag.Location): Int {
    var bits = 0x0
    
    for (flag in set) {
      if (!flag.locations().contains(location)) {
        throw IllegalArgumentException("unexpected flag: $flag use in target location: $location")
      }
      
      bits = bits or flag.mask()
    }
    
    return bits
  }
}

fun AccessFlagSet(vararg flags: AccessFlag): AccessFlagSet {
  return AccessFlagSet(ImmutableSet.from(flags))
}