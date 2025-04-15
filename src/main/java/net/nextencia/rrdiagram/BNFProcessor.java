// Copyright (c) YugabyteDB, Inc.

package net.nextencia.rrdiagram;

import net.nextencia.rrdiagram.common.Utils;
import net.nextencia.rrdiagram.grammar.model.*;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagram;
import net.nextencia.rrdiagram.grammar.rrdiagram.RRDiagramToSVG;

import java.util.logging.Logger;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;

class BNFProcessor {
  static Logger logger = Logger.getLogger(BNFProcessor.class.getName());

  static HashMap<String, BNFProcessor> processors = new HashMap<String, BNFProcessor>();

  String api;
  String version;
  Grammar grammar;

  public static synchronized void setDefault(BNFProcessor processor) {
    String key = String.format("%s-%s", "ysql", "preview");
    processors.put(key, processor);
  }

  // get the correct processor
  public static synchronized BNFProcessor get(String api, String version) {
    if (api == null) {
      api = "ysql";
    }

    if (version == null) {
      version = "preview";
    }

    String key = String.format("%s-%s", api, version);
    if (processors.containsKey(key)) {
      return processors.get(key);
    }

    // else load the file
    String bnffile = String.format("content/%s/api/%s/syntax_resources/%s_grammar.ebnf", version, api, api);
    File f = new File(bnffile);
    if (f.exists() && !f.isDirectory()) {
      processors.put(key, new BNFProcessor(bnffile, api, version));
      return processors.get(key);
    } else {
      logger.severe("Unable to locate grammar file: " + bnffile);
      return null;
    }
  }

  public BNFProcessor(String bnffile, String api, String version) {
    try {
      FileReader in = new java.io.FileReader(bnffile);
      BNFToGrammar btg = new BNFToGrammar();
      grammar = btg.convert(in);
      logger.info("loaded: " + bnffile);
      this.api = api;
      this.version = version;
    } catch (java.io.FileNotFoundException fne) {
      logger.severe("File not found: " + bnffile);
      System.exit(1);
    } catch (java.io.IOException ioe) {
      logger.severe("Unable to read from file: " + bnffile);
      System.exit(1);
    }
  }

  public String getReferenceFile() {
    StringBuilder sb = new StringBuilder();

    try {
      checkGrammar();

      GrammarToBNF bnf_builder = new GrammarToBNF();
      GrammarToRRDiagram.RuleLinkProvider ruleLinkProvider = new GrammarToRRDiagram.RuleLinkProvider() {
        private String toLinkName(String name) {
          return "../grammar_diagrams#" + name.replaceAll("_", "-");
        }

        @Override
        public String getLink(String ruleName) {
          return toLinkName(ruleName);
        }
      };

      for (Rule rule : grammar.getRules()) {
        try {
          sb.append("### " + rule.getName() + "\n");

          sb.append("```output.ebnf\n");
          sb.append(rule.toYBNF());
          sb.append("\n```\n");
          GrammarToRRDiagram diagram_builder = new GrammarToRRDiagram();
          diagram_builder.setRuleLinkProvider(ruleLinkProvider);
          diagram_builder.setRuleConsideredAsLineBreak(Utils.lineBreakRule);

          RRDiagram diagram = diagram_builder.convert(rule);
          String svg_string = new RRDiagramToSVG().convert(diagram);
          sb.append(svg_string);
          sb.append("\n\n");
        } catch (Exception e) {
          logger.severe("Exception occurred while exporting rule " + rule.getName());
          logger.warning(rule.toBNF(bnf_builder));
          throw e;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  public String getGrammar(List<Rule> targetRules) {
    StringBuilder sb = new StringBuilder();
    try {
      for (int i = 0; i < targetRules.size(); i++) {
        if (i > 0) {
          sb.append("\n");
        }
        sb.append(targetRules.get(i).toYBNF());
        sb.append("\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  String getGlobalPrefix(int depth) {
    /**
     * This function returns the relative of the grammar_diagrams file to the
     * current .md file
     * reference file @
     * content/preview/api/ysql/syntax_resources/grammar_diagrams.md
     * ysql/ycql is at depth 4 (from content)
     */

    String path = "";

    if (depth < 4) {
      path = "/" + version + "/api/" + api + "/";
    }

    // for every level deeper from ysql/ycql, add a ../
    switch (depth) {
      case 5:
        path = "../";
        break;
      case 6:
        path = "../../";
        break;
      case 7:
        path = "../../../";
        break;
      case 8:
        path = "../../../../";
        break;
      case 9:
        path = "../../../../../";
        break;
    }

    if (api.equals("ysql")) {
      return path + "syntax_resources/grammar_diagrams";
    } else {
      return path + "grammar_diagrams";
    }
  }

  public String getDiagram(List<Rule> targetRules, List<Rule> localRules, int depth) {
    StringBuilder sb = new StringBuilder();
    try {
      final Set<String> localRefs = new HashSet<String>();

      // current rules
      for (Rule rule : targetRules) {
        localRefs.add(rule.getName());
      }

      // other rules in the same page
      for (Rule rule : localRules) {
        localRefs.add(rule.getName());
      }

      String globalPrefix = getGlobalPrefix(depth);
      GrammarToRRDiagram.RuleLinkProvider ruleLinkProvider = new GrammarToRRDiagram.RuleLinkProvider() {
        @Override
        public String getLink(String ruleName) {
          String linkName = "#" + ruleName.replaceAll("_", "-");
          if (!localRefs.contains(ruleName)) {
            linkName = globalPrefix + linkName;
          }
          return linkName;
        }
      };

      for (Rule rule : targetRules) {
        sb.append("#### " + rule.getName());
        sb.append("\n\n");
        GrammarToRRDiagram diagram_builder = new GrammarToRRDiagram();
        diagram_builder.setRuleLinkProvider(ruleLinkProvider);
        diagram_builder.setRuleConsideredAsLineBreak(Utils.lineBreakRule);
        RRDiagram diagram = diagram_builder.convert(rule);
        String svg_string = new RRDiagramToSVG().convert(diagram);
        sb.append(svg_string);
        sb.append("\n\n");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return sb.toString();
  }

  public Set<String> getRuleNames() {
    Set<String> ruleNames = new HashSet<String>();
    ruleNames.add(Utils.lineBreakRule);
    for (Rule rule : grammar.getRules()) {
      if (!ruleNames.add(rule.getName())) {
        logger.warning("Rule defined multiple times: " + rule.getName());
      }
    }

    return ruleNames;
  }

  void checkGrammar() {
    Set<String> ruleNames = getRuleNames();

    for (Rule rule : grammar.getRules()) {
      Set<String> refs = rule.getUndefinedRuleRefs(ruleNames);
      if (!refs.isEmpty()) {
        logger.warning("Undefined rules referenced in rule '" + rule.getName() + "': " + refs.toString());
      }
    }
  }

  public List<Rule> getTargetRules(String[] targetRuleNames) {
    List<Rule> targetRules = new ArrayList<Rule>();
    if (targetRuleNames == null) {
      return targetRules;
    }

    try {
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
          logger.severe("Invalid target rule: " + targetRuleName);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return targetRules;
  }
}
