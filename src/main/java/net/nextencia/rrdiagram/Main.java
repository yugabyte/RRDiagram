package net.nextencia.rrdiagram;

import net.nextencia.rrdiagram.common.Utils;
import net.nextencia.rrdiagram.grammar.model.*;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagram;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagramToSVG;

import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class Main {
  public static void main(String[] args) throws Exception {

    if (args.length != 2) {
      System.out.println("Usage: java -jar rrdiagram.jar <input-file.ebnf> <output-file.md>");
      System.out.println("Example: ");
      System.out.println("java -jar rrdiagram.jar yb_pgsql_grammar.ebnf ~/code/docs/content/latest/api/ysql/grammar_diagrams_new.md");
      System.exit(1);
    }

    String inFileName = args[0];
    String outFileName = args[1];

    FileReader in = new java.io.FileReader(inFileName);
    BNFToGrammar btg = new BNFToGrammar();
    Grammar grammar = btg.convert(in);

    GrammarToRRDiagram.RuleLinkProvider ruleLinkProvider =
        new GrammarToRRDiagram.RuleLinkProvider() {
          private String toLinkName(String name) {
            return "../grammar_diagrams#" + name.replaceAll("_", "-");
          }

          @Override
          public String getLink(String ruleName) {
            return toLinkName(ruleName);
          }
        };
    PrintWriter pw = new java.io.PrintWriter(new java.io.File(outFileName));

    checkGrammar(grammar);

    for (Rule rule : grammar.getRules()) {
      pw.write("### " + rule.getName() + "\n");

      /*
        // Debug mode.
        GrammarToBNF bnf_builder = new GrammarToBNF();
        pw.write("```\n");
        pw.write(rule.toBNF(bnf_builder));
        pw.write("\n```\n");
      */

      pw.write("```\n");
      pw.write(rule.toYBNF());
      pw.write("\n```\n");

      GrammarToRRDiagram diagram_builder = new GrammarToRRDiagram();
      diagram_builder.setRuleLinkProvider(ruleLinkProvider);
      diagram_builder.setRuleConsideredAsLineBreak(Utils.lineBreakRule);

      RRDiagram diagram = diagram_builder.convert(rule);
      String svg_string = new RRDiagramToSVG().convert(diagram);
      pw.write(svg_string);
      pw.write("\n\n");
    }

    pw.close();
  }

  private static void checkGrammar(Grammar grammar) {
    Set<String> ruleNames = new HashSet<String>();
    ruleNames.add(Utils.lineBreakRule);
    for (Rule rule : grammar.getRules()) {
      if (!ruleNames.add(rule.getName())) {
        logWarn("Rule defined multiple times: " + rule.getName());
      }
    }

    for (Rule rule : grammar.getRules()) {
      Set<String> refs = rule.getUndefinedRuleRefs(ruleNames);
      if (!refs.isEmpty()) {
        logWarn("Undefined rules referenced in rule '" + rule.getName() + "': " + refs.toString());
      }
    }
  }

  private static void logWarn(String s) {
    System.out.println("WARNING: " + s);
  }

}
