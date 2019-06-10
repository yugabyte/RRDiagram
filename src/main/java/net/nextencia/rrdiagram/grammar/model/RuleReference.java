/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import net.nextencia.rrdiagram.common.Utils;
import net.nextencia.rrdiagram.grammar.model.GrammarToRRDiagram.RuleLinkProvider;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRBreak;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRText.Type;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Christopher Deckers
 */
public class RuleReference extends Expression {

  private String ruleName;

  public RuleReference(String ruleName) {
    this.ruleName = ruleName;
  }

  public String getRuleName() {
    return ruleName;
  }

  @Override
  protected RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram) {
    String ruleConsideredAsLineBreak = grammarToRRDiagram.getRuleConsideredAsLineBreak();
    if(ruleConsideredAsLineBreak != null && ruleConsideredAsLineBreak.equals(ruleName)) {
      return new RRBreak();
    }
    RuleLinkProvider ruleLinkProvider = grammarToRRDiagram.getRuleLinkProvider();
    return new RRText(Type.RULE, ruleName, ruleLinkProvider == null? null: ruleLinkProvider.getLink(ruleName));
  }

  @Override
  protected void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested) {
    sb.append(ruleName);
    String ruleConsideredAsLineBreak = grammarToBNF.getRuleConsideredAsLineBreak();
    if(ruleConsideredAsLineBreak != null && ruleConsideredAsLineBreak.equals(ruleName)) {
      sb.append("\n");
    }
  }

  @Override
  public void toYBNF(StringBuilder sb, boolean isNested) {
    if(ruleName.equals(Utils.lineBreakRule)) {
      sb.append("\n");
    } else {
      sb.append(ruleName);
    }
  }

  @Override
  public Set<String> getUndefinedRuleRefs(Set<String> rules) {
    Set<String> refs = new HashSet<String>();
    if (!rules.contains(ruleName)) {
      refs.add(ruleName);
    }
    return refs;
  }

  @Override
  public boolean equals(Object o) {
    if(!(o instanceof RuleReference)) {
      return false;
    }
    return ruleName.equals(((RuleReference)o).ruleName);
  }

}
