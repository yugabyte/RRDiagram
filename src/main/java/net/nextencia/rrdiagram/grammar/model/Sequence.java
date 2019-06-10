/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.grammar.model;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.nextencia.rrdiagram.common.Utils;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRElement;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRLoop;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRSequence;

/**
 * @author Christopher Deckers
 */
public class Sequence extends Expression {

  private Expression[] expressions;

  public Sequence(Expression... expressions) {
    this.expressions = expressions;
  }

  public Expression[] getExpressions() {
    return expressions;
  }

  @Override
  protected RRElement toRRElement(GrammarToRRDiagram grammarToRRDiagram) {
    List<RRElement> rrElementList = new ArrayList<RRElement>();
    for(int i=0; i<expressions.length; i++) {
      Expression expression = expressions[i];
      RRElement rrElement = expression.toRRElement(grammarToRRDiagram);
      // Treat special case of: "e (',' e)*" and "e (e)*"
      if(i < expressions.length - 1 && expressions[i + 1] instanceof Repetition) {
        Repetition repetition = (Repetition)expressions[i + 1];
        Expression repetitionExpression = repetition.getExpression();
        // Treat special case of: e (e)*
        if(expression.equals(repetitionExpression)) {
          Integer maxRepetitionCount = repetition.getMaxRepetitionCount();
          if(maxRepetitionCount == null || maxRepetitionCount > 1) {
            rrElement = new RRLoop(expression.toRRElement(grammarToRRDiagram), null, repetition.getMinRepetitionCount(), (maxRepetitionCount == null? null: maxRepetitionCount));
            i++;
          }
        } else if(repetitionExpression instanceof Sequence) {
          // Treat special case of: a (',' a)*
          Expression[] subExpressions = ((Sequence)repetitionExpression).getExpressions();
          if(subExpressions.length == 2 && subExpressions[0] instanceof Literal && subExpressions[1].equals(expression)) {
            Integer maxRepetitionCount = repetition.getMaxRepetitionCount();
            if(maxRepetitionCount == null || maxRepetitionCount > 1) {
              rrElement = new RRLoop(expression.toRRElement(grammarToRRDiagram), subExpressions[0].toRRElement(grammarToRRDiagram), repetition.getMinRepetitionCount(), (maxRepetitionCount == null? null: maxRepetitionCount));
              i++;
            }
          }
        }
      }
      rrElementList.add(rrElement);
    }
    return new RRSequence(rrElementList.toArray(new RRElement[0]));
  }

  @Override
  protected void toBNF(GrammarToBNF grammarToBNF, StringBuilder sb, boolean isNested) {
    if(expressions.length == 0) {
      sb.append("( )");
      return;
    }
    if(isNested && expressions.length > 1) {
      sb.append("( ");
    }
    boolean isCommaSeparator = grammarToBNF.isCommaSeparator();
    for(int i=0; i<expressions.length; i++) {
      if(i > 0) {
        if(isCommaSeparator) {
          sb.append(" ,");
        }
        sb.append(" ");
      }
      expressions[i].toBNF(grammarToBNF, sb, expressions.length == 1 && isNested || !isCommaSeparator);
    }
    if(isNested && expressions.length > 1) {
      sb.append(" )");
    }
  }

  @Override
  public void toYBNF(StringBuilder sb, boolean isNested) {
    if(expressions.length == 0) {
      sb.append("( )");
      return;
    }
    List<Expression> expressionList = Arrays.asList(expressions);
    if(isNested) {
      Utils.exprListToYBNF(sb, expressionList, "( "," "," )");
    } else {
      Utils.exprListToYBNF(sb, expressionList, ""," ","");
    }
  }

  @Override
  public Set<String> getUndefinedRuleRefs(Set<String> rules) {
    Set<String> refs = new HashSet<String>();
    for (Expression expression : expressions) {
      refs.addAll(expression.getUndefinedRuleRefs(rules));
    }
    return refs;
  }

  @Override
  public boolean equals(Object o) {
    if(!(o instanceof Sequence)) {
      return false;
    }
    return Arrays.equals(expressions, ((Sequence)o).expressions);
  }

}
