/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.nextencia.rrdiagram.common.Utils;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRChoice;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;

/**
 * @author Christopher Deckers
 */
public class Choice extends Expression {

  private Expression[] expressions;

  public Choice(Expression... expressions) {
    this.expressions = expressions;
  }

  public Expression[] getExpressions() {
    return expressions;
  }

  @Override
  protected RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram) {
    RRElement[] rrElements = new RRElement[expressions.length];
    for(int i=0; i<rrElements.length; i++) {
      rrElements[i] = expressions[i].toRRElement(grammarToRRDiagram);
    }
    return new RRChoice(rrElements);
  }

  @Override
  protected void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested) {
    List<Expression> expressionList = new ArrayList<Expression>();
    boolean hasNoop = false;
    for(Expression expression: expressions) {
      if(expression instanceof Sequence && ((Sequence)expression).getExpressions().length == 0) {
        hasNoop = true;
      } else {
        expressionList.add(expression);
      }
    }
    if(expressionList.isEmpty()) {
      sb.append("( )");
    } else if(hasNoop && expressionList.size() == 1) {
      boolean isUsingMultiplicationTokens = grammarToBNF.isUsingMultiplicationTokens();
      if(!isUsingMultiplicationTokens) {
        sb.append("[ ");
      }
      expressionList.get(0).toBNF(grammarToBNF, sb, isUsingMultiplicationTokens);
      if(!isUsingMultiplicationTokens) {
        sb.append(" ]");
      }
    } else {
      boolean isUsingMultiplicationTokens = grammarToBNF.isUsingMultiplicationTokens();
      if(hasNoop && !isUsingMultiplicationTokens) {
        sb.append("[ ");
      } else if(hasNoop || isNested && expressionList.size() > 1) {
        sb.append("( ");
      }
      int count = expressionList.size();
      for(int i=0; i<count; i++) {
        if(i > 0) {
          sb.append(" | ");
        }
        expressionList.get(i).toBNF(grammarToBNF, sb, false);
      }
      if(hasNoop && !isUsingMultiplicationTokens) {
        sb.append(" ]");
      } else if(hasNoop || isNested && expressionList.size() > 1) {
        sb.append(" )");
        if(hasNoop) {
          sb.append("?");
        }
      }
    }
  }

  @Override
  public void toYBNF(StringBuilder sb, boolean isWrapped) {
    List<Expression> expressionList = new ArrayList<Expression>();
    boolean hasNoop = false;
    for(Expression expression: expressions) {
      if(expression instanceof Sequence && ((Sequence)expression).getExpressions().length == 0) {
        hasNoop = true;
      } else {
        expressionList.add(expression);
      }
    }

    if (hasNoop) {
      // This is an optional choice: e.g. "[ e1 | e2 | e2 ]"
      Utils.exprListToYBNF(sb, expressionList, "[ ", " | ", " ]", isWrapped);
    } else {
      if (isWrapped && expressionList.size() > 1) {
        // Regular case: "e1 | e2 | e2"
        Utils.exprListToYBNF(sb, expressionList, "", " | ", "", isWrapped);
      } else {
        // Needs wrapping: e.g. "{ e1 | e2 | e2 }"
        Utils.exprListToYBNF(sb, expressionList, "{ ", " | ", " }", isWrapped);
      }
    }
  }

  @Override
  public Set<String> getUndefinedRuleRefs(Set<String> rules) {
    Set<String> refs = new HashSet<String>();
    for (Expression expression: expressions) {
      refs.addAll(expression.getUndefinedRuleRefs(rules));
    }
    return refs;
  }

  @Override
  public boolean equals(Object o) {
    if(!(o instanceof Choice)) {
      return false;
    }
    return Arrays.equals(expressions, ((Choice)o).expressions);
  }

}
