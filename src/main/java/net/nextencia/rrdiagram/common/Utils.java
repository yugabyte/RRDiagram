/*
 * Christopher Deckers (chrriis@nextencia.net)
 * http://www.nextencia.net
 *
 * See the file "readme.txt" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
package net.nextencia.rrdiagram.common;

import net.nextencia.rrdiagram.grammar.model.Expression;

import java.awt.Color;
import java.awt.Font;


/**
 * @author Christopher Deckers
 */
public class Utils {

  private Utils() {}

  public static String escapeXML(String s) {
    if(s == null || s.length() == 0) {
      return s;
    }
    StringBuilder sb = new StringBuilder((int)(s.length() * 1.1));
    for(int i=0; i<s.length(); i++) {
      char c = s.charAt(i);
      switch(c) {
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '&':
          sb.append("&amp;");
          break;
        case '\'':
          sb.append("&apos;");
          break;
        case '\"':
          sb.append("&quot;");
          break;
        default:
          sb.append(c);
        break;
      }
    }
    return sb.toString();
  }

  public static String convertColorToHtml(Color c) {
    StringBuilder connectorColorSB = new StringBuilder("#");
    if (c.getRed() < 16) {
      connectorColorSB.append('0');
    }
    connectorColorSB.append(Integer.toHexString(c.getRed()));
    if (c.getGreen() < 16) {
      connectorColorSB.append('0');
    }
    connectorColorSB.append(Integer.toHexString(c.getGreen()));
    if (c.getBlue() < 16) {
      connectorColorSB.append('0');
    }
    connectorColorSB.append(Integer.toHexString(c.getBlue()));
    String connectorColor = connectorColorSB.toString();
    return connectorColor;
  }

  public static String convertFontToCss(Font font) {
    StringBuilder sb = new StringBuilder();
    sb.append("font-family:").append(font.getFamily()).append(",Sans-serif;");
    if(font.isItalic()) {
      sb.append("font-style:italic;");
    }
    if(font.isBold()) {
      sb.append("font-weight:bold;");
    }
    sb.append("font-size:" + font.getSize() + "px;");
    return sb.toString();
  }


  public static void exprListToYBNF(StringBuilder sb,
                                    java.util.List<Expression> exprs,
                                    String start,
                                    String sep,
                                    String end) {
    sb.append(start);
    for (int i = 0; i < exprs.size(); i++) {
      if (i > 0) {
        sb.append(sep);
      }
      exprs.get(i).toYBNF(sb,false);
    }
    sb.append(end);
  }

  public static void exprRepToYBNF(StringBuilder sb,
                                    Expression expr,
                                    int repCount,
                                    String start,
                                    String sep,
                                    String end) {
    sb.append(start);
    for (int i = 0; i < repCount; i++) {
      if (i > 0) {
        sb.append(sep);
      }
      expr.toYBNF(sb,false);
    }
    sb.append(end);
  }

  // "Rule" considered as line-break in YBNF grammar.
  public static String lineBreakRule = "\\";

}
