import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Function;

public class CodeTest {
  interface Interface {
    void out();
  }

  public static final class Nested {
    public int j = 255;
  }

  public int i = 114514;

  public CodeTest(int i) {
    this.i = i;
    Runnable lambdaInCon = () -> System.out.println();
  }

  public void fail() {
    String what = (String) new Object();
  }

  public void bar() {
    System.out.println("bar");
  }

  public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException {
    boolean z = true;
    boolean zz = false;
    boolean zzz = !z;
    byte b = 127;
    short s = 128;
    int i = 114514;
    long j = 1919810;
    float f = 0.1F;
    double d = 0.2D;
    char c = '1';
    boolean zzzz = i > j;
    byte[] ba = {42};
    long[] la = {42};
    Object[] oa = {new Object()};
    Object oao = oa[0];
    Object boxing = 810;
    Function<Integer, String> itoa = ii -> Long.toString(ii + j);
    var lookup = MethodHandles.publicLookup();
    var method_bar = lookup.findVirtual(CodeTest.class, "bar", MethodType.methodType(void.class));
    Objects.requireNonNull(byte.class.toString());
  }
}