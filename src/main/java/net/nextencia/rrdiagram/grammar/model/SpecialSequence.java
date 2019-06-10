/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText.Type;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Christopher Deckers
 */
public class SpecialSequence extends Expression {

  private String text;

  public SpecialSequence(String text) {
    this.text = text;
  }

  @Override
  protected RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram) {
    return new RRText(Type.SPECIAL_SEQUENCE, text, null);
  }

  @Override
  protected void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested) {
    sb.append("(? ");
    sb.append(text);
    sb.append(" ?)");
  }

  @Override
  public void toYBNF(StringBuilder sb, boolean isNested) {
    throw new UnsupportedOperationException("Special sequences not supported in YBNF");
  }

  @Override
  public Set<String> getUndefinedRuleRefs(Set<String> rules) {
    return new HashSet<String>();
  }

  @Override
  public boolean equals(Object o) {
    if(!(o instanceof SpecialSequence)) {
      return false;
    }
    return text.equals(((SpecialSequence)o).text);
  }

}
