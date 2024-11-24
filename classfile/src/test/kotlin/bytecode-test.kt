import kala.collection.Seq
import org.aya.tool.classfile.*
import org.aya.tool.classfile.CodeBuilderWrapper.Companion.ExprCont
import org.junit.jupiter.api.Test
import java.io.PrintStream
import java.lang.classfile.AccessFlags
import java.lang.classfile.ClassFile
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.AccessFlag
import java.nio.file.Files
import java.nio.file.Path

class BytecodeTest {
  companion object {
    val GEN_DIR: Path = Path.of("src", "test", "gen")
  }
  
  @Test
  fun test0() {
    val field_System_out = FieldData(
      System::class.java.asDesc(),
      AccessFlags.ofField(public().static().final().mask()),
      PrintStream::class.java.asDesc(),
      "out"
    )
    
    val method_PrintStream_println = MethodData(
      ClassData(PrintStream::class.java),
      "println",
      AccessFlags.ofMethod(public().mask()),
      MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String),
      false
    )
    
    val method_Boolean_valueOf = MethodData(
      ClassData(java.lang.Boolean::class.java),
      "valueOf",
      AccessFlags.ofMethod(public().static().mask()),
      MethodTypeDesc.of(ConstantDescs.CD_Boolean, ConstantDescs.CD_boolean),
      false
    )
    
    val method_Object_toString = MD_toString(ClassData(ConstantDescs.CD_Object))
    // Runnable#run : () -> ()
    val method_Runnable_run = MethodData(
      ClassData(Runnable::class.java),
      "run",
      AccessFlags.ofMethod(AccessFlag.PUBLIC, AccessFlag.ABSTRACT),
      MethodTypeDesc.of(ConstantDescs.CD_void),
      true
    )
    
    val bytecode = ClassData(ClassDesc.of("SomeMain")).build(ClassFile.of()) {
      val SomeMain = this.classData.className
      builder.withVersion(64, 0)
      
      val field_foo = public().field(ConstantDescs.CD_String, "foo")
      
      val SomeMain_nu = public().constructor(
        MD_Object_init, Seq.empty(),
        ConstantDescs.CD_Object
      ) { arg0 ->
        field_foo.of(self).set(method_Object_toString.of(arg0).invoke())
        val f = method_Runnable_run.lambda(self) {
          val self = it.capture(0)
          val out = field_System_out.get()
          +method_PrintStream_println.of(out).invoke(field_foo.of(self).get())
          ret()
        }
        
        +method_Runnable_run.of(f).invoke()
        ret()
      }
      
      val SomeMain_bar = public().method(ConstantDescs.CD_boolean, "bar") {
        val ref_foo = field_foo.of(self)
        val ifResult = ifThen(ref_foo.get().instanceof(ConstantDescs.CD_String), ExprCont(ConstantDescs.CD_boolean) {
          +method_PrintStream_println.of(field_System_out.get()).invoke(ref_foo.get())
          +CodeBuilderWrapper.ja
        }).orElse(ExprCont(ConstantDescs.CD_boolean) {
          ref_foo.set(CodeBuilderWrapper.nilRef)
          +CodeBuilderWrapper.nein
        }).expr()
        
        ret(ifResult)
      }
      
      main { args ->
        // var someMain = new SomeMain(new Object())
        val someMain = let(SomeMain)
        someMain.set(SomeMain_nu.nu(MD_Object_new.nu()))
        
        // var someMainResult = someMain.bar()
        val someMainResult = let(ConstantDescs.CD_boolean)
        someMainResult.set(SomeMain_bar.of(someMain).invoke())
        
        // System.out.println(
        +method_PrintStream_println.of(field_System_out.get()).invoke(
          // Boolean.valueOf(someMainResult).toString()
          method_Object_toString.of(method_Boolean_valueOf.invoke(someMainResult)).invoke()
        )
        // )
        
        ret()
      }
    }
    
    Files.write(GEN_DIR.resolve("SomeMain.class"), bytecode)
  }
}