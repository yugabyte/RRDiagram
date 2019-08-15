package net.nextencia.rrdiagram.common;

import net.nextencia.rrdiagram.grammar.model.Choice;
import net.nextencia.rrdiagram.grammar.model.Expression;
import net.nextencia.rrdiagram.grammar.model.Literal;

import java.util.Stack;
import java.util.List;

import static net.nextencia.rrdiagram.common.Utils.emptySep;

public class YBNFStringBuilder {
  static final private int SOFT_LINE_BREAK = 40;
  static final private int HARD_LINE_BREAK = 70;

  public enum BlockType {
    MULTILINE_CHOICE,
    DEFAULT
  }

  private StringBuilder sb;


  private Stack<String> indents;
  private Stack<BlockType> blocks;

  private int lastnewline;


  private boolean size_estimate_dry_run = false;

  public YBNFStringBuilder(String ruleName) {
    sb = new StringBuilder();
    lastnewline = sb.length();
    indents = new Stack<String>();
    blocks = new Stack<BlockType>();

    sb.append(ruleName);
    sb.append(" ::= ");
    startIndent();
  }

  public YBNFStringBuilder() {
    sb = new StringBuilder();
    lastnewline = sb.length();
    indents = new Stack<String>();
    blocks = new Stack<BlockType>();

    size_estimate_dry_run = true;
  }

  private void endIndent() {
    indents.pop();
  }

  private void startIndent() {
    indents.add(createIndent(currentLineLength()));
  }

  private int currentLineLength() {
    return sb.length() - lastnewline;
  }

  private String createIndent(int size) {
    return new String(new char[size]).replace("\0", " ");
  }

  public void append(String s) {
    sb.append(s);
  }

  public void append(Expression expr, boolean isWrapped) {
    if (size_estimate_dry_run) {
      int oldSize = sb.length();
      expr.toYBNF(this, isWrapped);
      expr.setYBNFSize(sb.length() - oldSize);
    } else {
      beginBlock(expr);
      expr.toYBNF(this, isWrapped);
      endBlock();
    }
  }

  private void beginBlock(Expression expr) {
    int exprSize = expr.getYBNFSize();

    boolean tooLong = currentLineLength() + exprSize > HARD_LINE_BREAK;
    int standaloneSize = indents.peek().length() + exprSize;
    boolean standaloneTooLong = standaloneSize > HARD_LINE_BREAK;

    BlockType blockType = BlockType.DEFAULT;
    if (tooLong) {
      if (expr instanceof Choice) {
        if (standaloneTooLong) {
          blockType = BlockType.MULTILINE_CHOICE;
          if (currentLineLength() > SOFT_LINE_BREAK ||
              standaloneSize > HARD_LINE_BREAK + SOFT_LINE_BREAK) {
            lineBreak();
          }
        } else {
          if (currentLineLength() > SOFT_LINE_BREAK) {
            lineBreak();
          } else {
            blockType = BlockType.MULTILINE_CHOICE;
          }
        }

      } else {
        lineBreak();
      }
    }

    blocks.add(blockType);
    startIndent();
  }

  private void endBlock() {
    blocks.pop();
    endIndent();
  }

  private boolean inMultilineChoiceBlock() {
    return !size_estimate_dry_run &&
        blocks.size() > 0 &&
        blocks.peek() == BlockType.MULTILINE_CHOICE;
  }

  public void appendExprList(List<Expression> exprs,
                             String start,
                             String sep,
                             String end,
                             boolean isWrapped) {
    append(start);

    boolean multiline_choice = inMultilineChoiceBlock();
    if (multiline_choice) {
      // Replace the indent with the one after the start marker.
      indents.pop();
      startIndent();
    }

    for (int i = 0; i < exprs.size(); i++) {
      /* Already wrapped if surrounding separators are non-empty */
      boolean isElemWrapped = !emptySep(sep) &&
          (i > 0  || isWrapped || !emptySep(start)) &&
          (i < exprs.size() - 1  || isWrapped || !emptySep(end));

      /* Additionally, for inner elems, check if surrounding elems are parenthesis */
      if (!isElemWrapped && i > 0 && i < exprs.size() - 1) {
        isElemWrapped = (exprs.get(i - 1) instanceof Literal) &&
            ((Literal) exprs.get(i - 1)).getText().equals("(") &&
            (exprs.get(i+1) instanceof Literal) &&
            ((Literal) exprs.get(i+1)).getText().equals("(");
      }

      append(exprs.get(i), isElemWrapped);

      if (i < exprs.size() - 1) {
        if (multiline_choice) {
          boolean appliedLineBreak = lineBreak();
          if (appliedLineBreak) {
            append(sep.replaceAll("^\\s+", ""));
          }
          else {
            append(sep);
          }
        } else {
          append(sep);
        }
      }
    }
    append(end);
  }

  public void appendRepExpr(Expression expr,
                            int repCount,
                            String start,
                            String sep,
                            String end,
                            boolean isWrapped) {
    append(start);
    for (int i = 0; i < repCount; i++) {
      if (i > 0) {
        append(sep);
      }

      /* Already wrapped if surrounding separators are non-empty */
      boolean isElemWrapped = !emptySep(sep) &&
          (i > 0 || isWrapped || !emptySep(start)) &&
          (i < repCount - 1 || isWrapped || !emptySep(end));

      append(expr, isElemWrapped);
    }
    sb.append(end);
  }

  private boolean lineBreak() {
    if (currentLineLength() >= 5 + indents.peek().length()) {
      sb.append("\n");
      lastnewline = sb.length();
      sb.append(indents.peek());
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return sb.toString();
  }
}


