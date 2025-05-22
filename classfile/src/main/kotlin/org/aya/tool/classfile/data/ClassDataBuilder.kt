package org.aya.tool.classfile.data

import kala.collection.SeqLike
import kala.collection.mutable.MutableList
import org.aya.tool.classfile.AccessFlagBuilder
import org.aya.tool.classfile.AccessFlagSet
import org.aya.tool.classfile.className
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.reflect.AccessFlag

/**
 * @param flags the access flag of the class
 * @param superClass the super class of this class, null if [Any].
 */
class ClassDataBuilder(
  private val outer: ClassData?,
  val flags: AccessFlagSet,
  private val name: ClassDesc,
  private var superClass: ClassRef? = null,
  private var interfaces: MutableList<ClassRef> = MutableList.create(),
) {
  init {
    if (!flags.isStatic && outer == null) {
      // inner but no outer
      throw IllegalArgumentException("missing outer class")
    }
  }
  
  /**
   * Specify the super class, note that any subsequence call will get an exception, even an [Any] is specified at the first time.
   */
  fun extends(ref: ClassRef) {
    if (superClass != null) throw IllegalStateException()
    if (ref.flags.has(AccessFlag.INTERFACE)) throw IllegalStateException("extends interface: $ref")
    superClass = ref
  }
  
  fun implements(vararg interfaces: ClassRef) {
    interfaces.forEach {
      if (!it.flags.has(AccessFlag.INTERFACE)) throw IllegalStateException("implements class: $it")
    }
    
    this.interfaces.appendAll(*interfaces)
  }
  
  fun implements(interfaces: SeqLike<ClassRef>) {
    interfaces.forEach {
      if (!it.flags.has(AccessFlag.INTERFACE)) throw IllegalStateException("implements class: $it")
    }
    
    this.interfaces.appendAll(interfaces)
  }
  
  fun buildInner(): InnerClassData {
    val built = build()
    if (built !is InnerClassData) throw IllegalStateException("static")
    return built
  }
  
  fun build(): ClassData {
    val superClass = superClass?.descriptor ?: ConstantDescs.CD_Object
    val interfaces = interfaces.map { it.descriptor }
    
    if (outer != null) {
      return DefaultInnerClassData(flags, outer, name.className(), superClass, interfaces)
    }
    
    return ClassData(flags, name, interfaces, superClass)
  }
}