import java.util.Objects;
import java.util.function.Function;

public class CodeTest {
  interface Interface {
    void out();
  }

  public static class Nested {
    public int j = 255;
  }

  public int i = 114514;

  public CodeTest(int i) {
    this.i = i;
  }

  public static void main(String[] args) {
    boolean z = true;
    boolean zz = false;
    byte b = 127;
    short s = 128;
    int i = 114514;
    long j = 1919810;
    float f = 0.1F;
    double d = 0.2D;
    char c = '1';
    byte[] ba = {42};
    long[] la = {42};
    Object[] oa = {new Object()};
    Function<Integer, String> itoa = ii -> Long.toString(ii + j);
    Objects.requireNonNull(byte.class.toString());
  }
}