package org.github.hoshinotented.mct

import kala.collection.immutable.ImmutableSeq
import kala.collection.immutable.primitive.ImmutableIntSeq
import org.aya.pretty.doc.Doc
import org.aya.pretty.doc.Docile

data class MCT(val on: Int, val name: String, val clauses: ImmutableSeq<Clause>) : Docile {
  sealed interface Body : Docile {
    data class Just(val indices: ImmutableIntSeq) : Body {
      override fun toDoc(): Doc {
        return Doc.plain(indices.joinToString(", "))
      }
    }
    
    data class Case(val mct: MCT) : Body {
      override fun toDoc(): Doc {
        return mct.toDoc()
      }
    }
  }
  
  /**
   * @param body a real clause body/index or another MCT
   */
  data class Clause(val case: Constructor, val body: Body) : Docile {
    override fun toDoc(): Doc {
      return Doc.sep(Doc.plain("|"), Doc.plain(case.name), Doc.plain("=>"), Doc.nest(2, body.toDoc()))
    }
  }
  
  override fun toDoc(): Doc {
    return Doc.vcat(
      Doc.sep(Doc.plain("case"), Doc.plain("$name($on)"), Doc.plain("{")),
      Doc.vcat(clauses.map { it.toDoc() }),
      Doc.plain("}")
    )
  }
}

fun build(on: Int, matching: Matching): MCT.Body {
  val param = matching.telescope.first
  val type = param.type
  val patterns = matching.clauses.map { it.patterns.first }
  
  if (patterns.allMatch { it is Pattern.Bind } || type is DataType.Indestructible) {
    val remain = matching.drop()
    
    // no more telescope
    if (remain == null) {
      return MCT.Body.Just(matching.clauses.view().map { it.id }.collect(ImmutableIntSeq.factory<Unit>()))
    }
    
    // skip splitting
    return build(on + 1, remain)
  }
  
  type as DataType.Destructible
  
  val cons = type.constructors
  val subTree = cons.map { case ->
    val conTele = case.telescope.mapIndexed { i, ty -> Parameter("${case.name}_$i", ty) }
    val newTele = matching.telescope.view().drop(1).prependedAll(conTele).toImmutableSeq()
    val newClauses = matching.clauses.mapNotNull {
      val innerPatterns = when (val pat = it.patterns.first) {
        // generate pattern for bind pattern
        is Pattern.Bind -> conTele.mapIndexed { i, _ -> Pattern.Bind("${pat.name}_$i") }
        is Pattern.Con -> if (pat.con == case) {
          pat.inner
        } else return@mapNotNull null
      }
      
      Matching.Clause(it.id, it.patterns.view().drop(1).prependedAll(innerPatterns).toImmutableSeq())
    }
    
    if (newTele.isEmpty) {
      MCT.Clause(case, MCT.Body.Just(newClauses.view().map { it.id }.collect(ImmutableIntSeq.factory<Unit>())))
    } else {
      val newMatching = Matching(newTele, newClauses)
      // +1 for the parameter we just splitting
      MCT.Clause(case, build(on + 1, newMatching))
    }
  }
  
  return MCT.Body.Case(MCT(on, param.name, subTree))
}