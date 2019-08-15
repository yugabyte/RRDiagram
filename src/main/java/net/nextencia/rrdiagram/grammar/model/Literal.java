/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import net.nextencia.rrdiagram.common.YBNFStringBuilder;
import net.nextencia.rrdiagram.grammar.model.GrammarToBNF.LiteralDefinitionSign;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText.Type;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Christopher Deckers
 */
public class Literal extends Expression {

  private String text;

  public Literal(String text) {
    this.text = text;
  }

  @Override
  protected RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram) {
    return new RRText(Type.LITERAL, text, null);
  }

  @Override
  protected void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested) {
    char c = grammarToBNF.getLiteralDefinitionSign() == LiteralDefinitionSign.DOUBLE_QUOTE? '"': '\'';
    sb.append(c);
    sb.append(text);
    sb.append(c);
  }

  @Override
  public void toYBNF(YBNFStringBuilder sb, boolean isWrapped) {
    // In YBNF uppercase implies literals (e.g. SELECT, INSERT, etc).
    boolean needs_quote = !text.equals(text.toUpperCase());
    if (needs_quote) {
      sb.append("'");
    }
    sb.append(text);
    if (needs_quote) {
      sb.append("'");
    }
  }

  public String getText() {
    return text;
  }

  @Override
  public Set<String> getUndefinedRuleRefs(Set<String> rules) {
    return new HashSet<String>();
  }

  @Override
  public boolean equals(Object o) {
    if(!(o instanceof Literal)) {
      return false;
    }
    return text.equals(((Literal)o).text);
  }
}
