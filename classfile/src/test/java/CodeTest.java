import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.function.Function;

public class CodeTest {
  @Retention(RetentionPolicy.CLASS)
  public @interface MyAnno {
    String value();
  }

  interface Interface {
    void out();

    default void defaultFn() {
      System.out.println("wow");
    }

    static void interfaceStatic() {
    }
  }

  public static final class Nested {
    public int j = 255;

    public static final class NestedNested {
    }
  }

  public final class Inner {
    public int j = i;
    public Object what = new Nested();
  }

  public static enum MyEnum {
    True, False;
  }

  public int i = 114514;

  public CodeTest(int i) {
    this.i = i;
  }

  public <R> R id(R v) {
    return v;
  }

  public void primitives() {
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
    boolean zzzzz = i == 0;
    boolean zzzzzz = null instanceof String;
    MyEnum ja = MyEnum.True;
    String str = "114514";
  }

  public void arrays() {
    byte[] ba = {42};
    long[] la = {42, 43};
    Object[] oa = {new Object()};
    Object oao = oa[0];
    int length = ba.length;
  }

  public void boxing() {
    Object boxing = 810;
  }

  public void lambda() {
    long j = 1919810;
    Function<Integer, String> itoa = ii -> Long.toString(ii + j);
  }

  public void invocation() {
    Objects.requireNonNull(byte.class.toString(), "ohno");
    System.out.println("bar");
    String what = (String) new Object();
    Interface.interfaceStatic();
  }

  public int switchCase(int i) {
    int result = 0;

    switch (i) {
      case 0:
      case 1:
      case 2:
        result = 1;
        break;
      case 3:
        result = 2;
        break;
      default:
        result = 3;
    }

    result = switch (result) {
      case 1 -> {
        System.out.println(1);
        yield -1;
      }
      case 2 -> {
        System.out.println(2);
        yield -2;
      }
      case 3 -> {
        System.out.println(3);
        yield -3;
      }
      default -> 0;
    };

    return result;
  }

  public void condition(boolean b) {
    int result = 0;
    if (b) {
      int local = 1;
      result = local;
    } else {
      String local = "2";
      result = Integer.valueOf(local);
    }
  }

  public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException {

  }
}