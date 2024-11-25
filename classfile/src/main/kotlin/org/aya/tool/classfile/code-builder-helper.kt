package org.aya.tool.classfile

import java.lang.classfile.CodeBuilder

fun CodeBuilder.iconst(i: Int) {
  when (i) {
    0 -> iconst_0()
    1 -> iconst_1()
    2 -> iconst_2()
    3 -> iconst_3()
    4 -> iconst_4()
    5 -> iconst_5()
    -1 -> iconst_m1()
    in Byte.MIN_VALUE..Byte.MAX_VALUE -> bipush(i)
    in Short.MIN_VALUE..Short.MAX_VALUE -> sipush(i)
    else -> ldc(constantPool().intEntry(i))
  }
}