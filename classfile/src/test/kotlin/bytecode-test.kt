import kala.collection.Seq
import org.aya.tool.classfile.*
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
      PrintStream::class.java.asDesc(),
      "println",
      AccessFlags.ofMethod(public().mask()),
      MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String),
      false
    )
    
    val Object_toString = MD_toString(ConstantDescs.CD_Object)
    // Runnable#run : () -> ()
    val Runnable_run = MethodData(
      Runnable::class.java.asDesc(),
      "run",
      AccessFlags.ofMethod(AccessFlag.PUBLIC, AccessFlag.ABSTRACT),
      MethodTypeDesc.of(ConstantDescs.CD_void),
      true
    )
    
    val bytecode = ClassData(ClassDesc.of("SomeMain")).build(ClassFile.of()) {
      builder.withVersion(64, 0)
      
      val field_foo = public().field(ConstantDescs.CD_String, "foo")
      
      val SomeMain_nu = public().constructor(
        MD_Object_init, Seq.empty(),
        ConstantDescs.CD_Object
      ) { arg0 ->
        field_foo.of(self).set(Object_toString.of(arg0).invoke())
        val f = Runnable_run.lambda(self) {
          val self = it.capture(0)
          val out = field_System_out.get()
          +method_PrintStream_println.of(out).invoke(field_foo.of(self).get())
          ret()
        }
        
        +Runnable_run.of(f).invoke()
        builder.return_()
      }
      
      main { args ->
        val obj = MD_Object_new.nu()
        +SomeMain_nu.nu(obj)
        ret()
      }
    }
    
    Files.write(GEN_DIR.resolve("SomeMain.class"), bytecode)
  }
}