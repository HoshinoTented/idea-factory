import Nat.suc
import Nat.zro
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableList
import org.github.hoshinotented.mct.*
import org.github.hoshinotented.mct.Constructor.Companion.invoke
import org.github.hoshinotented.mct.Pattern.Companion.check
import org.github.hoshinotented.mct.Pattern.Companion.invoke
import kotlin.test.Test

data object Nat : DataType.Destructible {
  val zro = "zro"()
  val suc = "suc"(Nat)
  
  override val constructors: ImmutableSeq<Constructor> = ImmutableSeq.of(zro, suc)
}

class MatchingBuilder(
  private val telescope: ImmutableSeq<Parameter>,
  private val clauses: MutableList<Matching.Clause> = MutableList.create(),
) {
  fun clause(vararg patterns: Any) {
    val realPatterns = check(patterns)
    clauses.append(Matching.Clause(clauses.size(), realPatterns))
  }
  
  fun build(): Matching {
    return Matching(telescope, clauses.toImmutableSeq())
  }
}

class Tester {
  @Test
  fun test0() {
    // def plus (a b : Nat) : Nat
    // | zro, b => b
    // | suc a, b => suc (plus a b)
    val matching = Matching(
      ImmutableSeq.of(Parameter("n", Nat), Parameter("m", Nat)), ImmutableSeq.of(
        Matching.Clause(0, ImmutableSeq.of(zro(), Pattern.Bind("b"))),
        Matching.Clause(1, ImmutableSeq.of(suc(Pattern.Bind("a")), Pattern.Bind("b"))),
      )
    )
    
    val result = build(0, matching)
    println(result.toDoc().debugRender())
  }
  
  @Test
  fun test1() {
    // def what (a b : Nat) : Nat
    // | zro, zro =>
    // | zro, suc zro =>
    // | zro, suc (suc b) =>
    // | suc a, zro =>
    // | suc a, suc b =>
    val matching = Matching(
      ImmutableSeq.of(Parameter("n", Nat), Parameter("m", Nat)), ImmutableSeq.of(
        Matching.Clause(0, ImmutableSeq.of(zro(), zro())),
        Matching.Clause(1, ImmutableSeq.of(zro(), suc(zro()))),
        Matching.Clause(2, ImmutableSeq.of(zro(), suc(suc(Pattern.Bind("b"))))),
        Matching.Clause(3, ImmutableSeq.of(suc(Pattern.Bind("a")), zro())),
        Matching.Clause(4, ImmutableSeq.of(suc(Pattern.Bind("a")), suc(Pattern.Bind("b")))),
      )
    )
    
    val result = build(0, matching)
    println(result.toDoc().debugRender())
  }
  
  @Test
  fun test2() {
    // def what (n m : Nat) : Nat
    // | zro, zro =>
    // | a, suc b =>
    // | suc a, suc b =>
    val matching = MatchingBuilder(ImmutableSeq.of(Parameter("n", Nat), Parameter("m", Nat))).apply {
      clause(zro(), zro())
      clause("a", suc("b"))
      clause(suc("a"), suc("b"))
    }.build()
    
    val result = build(0, matching)
    println(result.toDoc().debugRender())
  }
}
