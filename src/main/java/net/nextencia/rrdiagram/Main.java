package net.nextencia.rrdiagram;

import net.nextencia.rrdiagram.common.Utils;
import net.nextencia.rrdiagram.grammar.model.*;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagram;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagramToSVG;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

  private static void printHelpAndExit() {
    System.out.println("Usage: java -jar rrdiagram.jar <input-file.ebnf> <output-folder>");
    System.out.println("Will re-generate contents for each file in the output folder as follows:");
    System.out.println(" - grammar_diagrams.md: reference file with all grammar&diagram for all rules");
    System.out.println(" - *.diagram.md: diagrams for all rules (comma-separated) from file name");
    System.out.println(" - *.grammar.md: grammars for all rules (comma-separated) from file name");
    System.out.println("Other files with non-matching names will be ignored.");
    System.out.println("Example:");
    System.out.println("  java -jar rrdiagram.jar "
                       + "docs/content/latest/api/ysql/syntax_resources/ysql_grammar.ebnf "
                       + "docs/content/latest/api/ysql/syntax_resources/");
    System.exit(1);
  }

  public static void main(String[] args) throws Exception {

    if (args.length != 2) {
      System.out.println("Invalid number of arguments");
      printHelpAndExit();
    }

    if (!RRDiagramToSVG.isFontInstalled()) {
      logErr("Could not find font: " + RRDiagramToSVG.FONT_FAMILY_NAME);
      System.exit(1);
    }

    String inFileName = args[0];
    String outFolderName = args[1];
    File outFolder = new File(outFolderName);

    FileReader in = new java.io.FileReader(inFileName);
    BNFToGrammar btg = new BNFToGrammar();
    Grammar grammar = btg.convert(in);

    regenerateFolder(outFolder, grammar);
  }

  private static void regenerateFolder(File outFolder, Grammar grammar) throws Exception {
    File[] files = outFolder.listFiles();
    if (files == null) {
      logErr("Could not get files from subfolder: + " + outFolder);
      System.exit(1);
    }

    Arrays.sort(files);
    for (File file : files) {
      if (file.isDirectory()) {
        regenerateFolder(file, grammar);
      } else {
        String fileName = file.getName();
        if (fileName.equals("grammar_diagrams.md")) {
          logInfo("Re-generating reference file " + fileName);
          regenerateReferenceFile(grammar, file);
        } else {
          String[] comps = fileName.split("\\.");
          if (comps.length != 3 || !comps[2].equals("md")) {
            logWarn("Ignoring file '" + file.getCanonicalPath() + "'. ");
            continue;
          }

          List<Rule> targetRules = getTargetRules(comps[0].split(","), grammar);
          if (comps[1].equals("grammar")) {
            logInfo("Re-generating grammar file " + fileName);
            reGenerateGrammar(file, targetRules);
          } else if (comps[1].equals("diagram")) {
            logInfo("Re-generating diagram file " + fileName);
            reGenerateDiagram(file, targetRules);
          } else {
            logErr("Invalid export type '" + comps[1] + "' for file '" + file.getCanonicalPath() + "'. ");
            System.exit(1);
          }
        }
      }
    }
  }

  private static void regenerateReferenceFile(Grammar grammar, File outFile) throws Exception {
    checkGrammar(grammar);

    GrammarToBNF bnf_builder = new GrammarToBNF();
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

    PrintWriter pw = new java.io.PrintWriter(outFile);
    pw.write("---\n");
    pw.write("title: Grammar Diagrams\n");
    pw.write("summary: Diagrams of the grammar rules.\n");
    pw.write("---\n\n");

    for (Rule rule : grammar.getRules()) {
      try {
        pw.write("### " + rule.getName() + "\n");

        /*
          // Debug mode.
          pw.write("```output.ebnf\n");
          pw.write(rule.toBNF(bnf_builder));
          pw.write("\n```\n");
        */

        pw.write("```output.ebnf\n");
        pw.write(rule.toYBNF());
        pw.write("\n```\n");
        GrammarToRRDiagram diagram_builder = new GrammarToRRDiagram();
        diagram_builder.setRuleLinkProvider(ruleLinkProvider);
        diagram_builder.setRuleConsideredAsLineBreak(Utils.lineBreakRule);

        RRDiagram diagram = diagram_builder.convert(rule);
        String svg_string = new RRDiagramToSVG().convert(diagram);
        pw.write(svg_string);
        pw.write("\n\n");
      } catch (Exception e) {
        logWarn("Exception occurred while exporting rule " + rule.getName());
        logWarn(rule.toBNF(bnf_builder));
        throw e;
      }
    }
    pw.close();
  }

  private static void reGenerateGrammar(File outFile, List<Rule> targetRules) throws Exception {
    PrintWriter pw = new java.io.PrintWriter(outFile);
    pw.write("```output.ebnf\n");
    for (int i = 0; i < targetRules.size(); i++) {
      if (i > 0) {
        pw.write("\n");
      }
      pw.write(targetRules.get(i).toYBNF());
      pw.write("\n");
    }
    pw.write("```\n");
    pw.close();
  }

  private static String getGlobalRulePrefix(File diagFile) {
    StringBuilder sb = new StringBuilder();
    try {
      File file = diagFile.getCanonicalFile();
      while (file != null && !file.getName().equals("syntax_resources")) {
        sb.append("../");
        file = file.getParentFile();
      }
      if (file == null) {
        logErr("Invalid file path '" + diagFile + "'.\n"
               + "Expected to have an ancestor called 'syntax_resources'.");
        System.exit(1);
      }
    } catch (IOException exception) {
      logErr("Caught IOException while trying to get the canonical file of '"
             + diagFile + "': " + exception);
      System.exit(1);
    }
    sb.append("syntax_resources/grammar_diagrams");
    return sb.toString();
  }

  private static String getRuleLink(String ruleName,
                                    Set<String> localRules,
                                    String globalPrefix) {
    String linkName = "#" + ruleName.replaceAll("_", "-");
    if (!localRules.contains(ruleName)) {
      linkName = globalPrefix + linkName;
    }
    return linkName;
  }

  private static void reGenerateDiagram(final File outFile,
                                        List<Rule> targetRules) throws Exception {
    final Set<String> targetRuleNames = new HashSet<String>();
    for (Rule rule : targetRules) {
      targetRuleNames.add(rule.getName());
    }

    GrammarToRRDiagram.RuleLinkProvider ruleLinkProvider =
        new GrammarToRRDiagram.RuleLinkProvider() {
          @Override
          public String getLink(String ruleName) {
            return getRuleLink(ruleName, targetRuleNames, getGlobalRulePrefix(outFile));
          }
        };

    PrintWriter pw = new java.io.PrintWriter(outFile);
    for (Rule rule : targetRules) {
      pw.write("#### " + rule.getName());
      pw.write("\n\n");
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

  private static List<Rule> getTargetRules(String[] targetRuleNames,
                                           Grammar grammar) throws Exception {
    List<Rule> targetRules = new ArrayList<Rule>();
    for (String targetRuleName : targetRuleNames) {
      boolean found = false;
      for (Rule rule : grammar.getRules()) {
        if (rule.getName().equals(targetRuleName)) {
          targetRules.add(rule);
          found = true;
          break;
        }
      }
      if (!found) {
        logErr("Invalid target rule: " + targetRuleName);
        System.exit(1);
      }
    }

    return targetRules;
  }

  private static Set<String> getRuleNames(Grammar grammar) {
    Set<String> ruleNames = new HashSet<String>();
    ruleNames.add(Utils.lineBreakRule);
    for (Rule rule : grammar.getRules()) {
      if (!ruleNames.add(rule.getName())) {
        logWarn("Rule defined multiple times: " + rule.getName());
      }
    }

    return ruleNames;
  }

  private static void checkGrammar(Grammar grammar) {
    Set<String> ruleNames = getRuleNames(grammar);

    for (Rule rule : grammar.getRules()) {
      Set<String> refs = rule.getUndefinedRuleRefs(ruleNames);
      if (!refs.isEmpty()) {
        logWarn("Undefined rules referenced in rule '" + rule.getName() + "': " + refs.toString());
      }
    }
  }

  private static void logErr(String s) {
    System.out.println("ERROR: " + s);
  }

  private static void logWarn(String s) {
    System.out.println("WARNING: " + s);
  }

  private static void logInfo(String s) {
    System.out.println("INFO: " + s);
  }
}
