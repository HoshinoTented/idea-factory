package org.aya.tool.classfile

import java.lang.classfile.AccessFlags
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.AccessFlag

val MD_Object_init = defaultConstructorData(ClassData(ConstantDescs.CD_Object))
private const val toString_NAME = "toString"

fun defaultConstructorData(owner: ClassData): MethodData {
  return MethodData(
    owner,
    ConstantDescs.INIT_NAME,
    AccessFlags.ofMethod(AccessFlag.PUBLIC),
    ConstantDescs.MTD_void,
    false
  )
}

fun MD_toString(owner: ClassData): MethodData {
  // public String toString();
  return MethodData(
    owner, toString_NAME, AccessFlags.ofMethod(AccessFlag.PUBLIC),
    MethodTypeDesc.of(ConstantDescs.CD_String), false
  )
}