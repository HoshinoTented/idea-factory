package org.aya.tool.classfile

import java.lang.classfile.AccessFlags
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.AccessFlag

val MD_Object_init = defaultConstructorData(ConstantDescs.CD_Object)
private const val toString_NAME = "toString"

fun defaultConstructorData(owner: ClassDesc): MethodData {
  return MethodData(
    owner,
    ConstantDescs.INIT_NAME,
    AccessFlags.ofMethod(AccessFlag.PUBLIC),
    ConstantDescs.MTD_void,
    false
  )
}

fun MD_toString(owner: ClassDesc): MethodData {
  // public String toString();
  return MethodData(
    owner, toString_NAME, AccessFlags.ofMethod(AccessFlag.PUBLIC),
    MethodTypeDesc.of(ConstantDescs.CD_String), false
  )
}