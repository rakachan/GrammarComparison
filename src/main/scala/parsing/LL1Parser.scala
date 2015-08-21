package parsing

import grammar.CFGrammar._
import scala.annotation.tailrec
import grammar.GrammarUtils
import grammar.GlobalContext

class LL1Parser(g : Grammar) extends Parser {
  case class EndOfRule(r: Rule)

  def parse(s: List[Terminal])(implicit opctx: GlobalContext): Boolean = {
    parseWithTree(s).nonEmpty
  }
  //Returns one possible grammar list
  def parseWithTree(s: List[Terminal])(implicit opctx: GlobalContext): Option[ParseTree] = {
    require(GrammarUtils.isLL1(g))

    val (nullable, first, follow) = GrammarUtils.nullableFirstFollow(g)
    //TODO: Compute the parsing table: Depending on the symbol we parse, which is the rule that we are going to apply.
    val nonTerminals = g.nonTerminals
    val parseTableBuild = for (
      nt <- nonTerminals;
      rule <- g.nontermToRules(nt);
      t <- GrammarUtils.firstA(rule.rightSide, nullable, first) ++ (if (rule.rightSide.forall(nullable)) follow(nt) else Nil)
    ) yield (nt, t) -> rule

    val parseTable: Map[(Nonterminal, Terminal), Rule] = parseTableBuild.toMap

    @tailrec def rec(current: List[Either[Symbol, EndOfRule]], s: List[Terminal], acc: List[ParseTree]): Option[ParseTree] = (current, s) match {
      case (Nil, Nil) => acc.headOption
      case (Right(EndOfRule(rule)) :: q, s) =>
        val n = rule.rightSide.length
        rec(q, s, Node(rule, acc.take(n).reverse) :: acc.drop(n))
      case (Left(nt: Nonterminal) :: q, a :: b) => parseTable.get((nt, a)) match {
        case Some(parsing) =>
          rec(parsing.rightSide.map(Left.apply) ++ List(Right(EndOfRule(parsing))) ++ q, s, acc)
        case _ => None
      }
      case (Left(t: Terminal) :: q, a :: b) if t == a =>
        rec(q, b, Leaf(t) :: acc)
    }
    rec(Left(g.start) :: Nil, s, Nil)
  }
}