/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;

import java.util.Set;

/**
 * @author Christopher Deckers
 */
public abstract class Expression {

  protected abstract RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram);

  protected abstract void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested);

  public abstract void toYBNF(StringBuilder sb, boolean isWrapped);

  public abstract Set<String> getUndefinedRuleRefs(Set<String> rules);

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    toBNF(new GrammarToBNF(), sb, false);
    return sb.toString();
  }
}
