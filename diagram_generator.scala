import net.nextencia.rrdiagram.grammar.model._
import net.nextencia.rrdiagram.grammar.rrdiagram._

/**
  * @param inFileName the ebnf file containing the declared grammar
  *                e.g. "/path/to/RRDiagram/ybsql_grammar.ebnf"
  * @param outFileName the markdown file where the geenrated diagrams will be written.
  *                e.g. "path/to/docs/content/api/cassandra/grammar_diagrams_new.md"
  */
def generate(inFileName : String, outFileName : String) {
  val in = new java.io.FileReader(inFileName)
  val btg = new BNFToGrammar()
  val grammar = btg.convert(in)
  val bnf_builder = new GrammarToBNF()

  var ruleLinkProvider: GrammarToRRDiagram.RuleLinkProvider = new GrammarToRRDiagram.RuleLinkProvider() {
    private def toLinkName(name: String) = "../grammar_diagrams#" + name.replaceAll("_", "-")
    override def getLink(ruleName: String) = toLinkName(ruleName)
  }

  val pw = new java.io.PrintWriter(new java.io.File(outFileName))
  pw.write("---\ntitle: Grammar Diagrams\nsummary: Diagrams of the grammar rules.\n---\n")

  for (rule <- grammar.getRules) {
    pw.write("### " + rule.getName() + "\n")
    pw.write("```\n")
    pw.write(rule.toBNF(bnf_builder))
    pw.write("\n```\n")

    val diagram_builder = new GrammarToRRDiagram()
    diagram_builder.setRuleLinkProvider(ruleLinkProvider)
    diagram_builder.setRuleConsideredAsLineBreak("\\")
    
    val diagram = diagram_builder.convert(rule)
    val svg_string = new RRDiagramToSVG().convert(diagram)
    pw.write(svg_string)
    pw.write("\n\n")
  }
  pw.close()
}
