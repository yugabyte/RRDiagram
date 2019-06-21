RRDiagram
=========

Generate railroad diagrams from code or BNF. Generate BNF from code.

RR Diagram is a Java library that generates railroad diagrams (also called syntax diagrams) from code or from BNF notation. The output format is a very compact SVG image which can be integrated to web pages and where rules can contain links.

RR Diagram can also be used to generate BNF notation from a model.

The generated output was inspired from this online-only version: http://railroad.my28msec.com/rr/ui

Binaries can be found on the [SourceForge page](https://sourceforge.net/projects/rrdiagram/).

Example
=======

This is the kind of diagrams that can get generated:
![H2 Select](http://rrdiagram.sourceforge.net/H2Select.svg)

The above is generated using the right conversion options on this BNF:
<pre>
H2_SELECT = 
'SELECT' [ 'TOP' term ] [ 'DISTINCT' | 'ALL' ] selectExpression {',' selectExpression} \
'FROM' tableExpression {',' tableExpression} [ 'WHERE' expression ] \
[ 'GROUP BY' expression {',' expression} ] [ 'HAVING' expression ] \
[ ( 'UNION' [ 'ALL' ] | 'MINUS' | 'EXCEPT' | 'INTERSECT' ) select ] [ 'ORDER BY' order {',' order} ] \
[ 'LIMIT' expression [ 'OFFSET' expression ] [ 'SAMPLE_SIZE' rowCountInt ] ] \
[ 'FOR UPDATE' ];
</pre>

Usage
=======

Generate new syntax/diagrams when updating the docs as follows:

1. Update the appropriate (i.e. `yb_cql_grammar.ebnf` or `yb_pgsql_grammar.ebnf`) source grammar file with your changes (e.g. adding a new supported statement or clause).
2. Get the executable jar

```bash
 wget https://github.com/YugaByte/RRDiagram/releases/download/0.9.4/rrdiagram.jar
```
_Note: Alternative build manually as described in the [Build](#build) section below (and move/rename the resulting jar from the target folder)._

3. Run the diagram generator:

```bash
java -jar rrdiagram.jar <input-file.ebnf> <output-file.md>
```
_Note: run `java -jar rrdiagram.jar` (without arguments) to see help._

4. Update the corresponding (i.e. `YSQL` or `YCQL`) `grammar_diagrams.md` file in our docs repository based on changes in the newly generated `grammar_diagrams.md`. Typically you can just replace the old version with the newly generated one.

5. Copy the newly-modified generated diagrams and syntax elements to any relevant files/sections in our documentation (e.g. the `select` into the `dml_select.md` file).

6. After copying the diagrams check that all links (for rule references) work. If they don't, manually replace the link paths as needed (e.g. `../../grammar_diagrams#<...>`).

Build
=====

```bash
mvn package -DskipTests=true
```

Internals
=========

The diagram model represents the actual constructs visible on the diagram.
To convert a diagram model to SVG:
```Java
RRDiagram rrDiagram = new RRDiagram(rrElement);
RRDiagramToSVG rrDiagramToSVG = new RRDiagramToSVG();
String svg = rrDiagramToSVG.convert(rrDiagram);
```

The grammar model represents a BNF-like grammar.
It can be converted to a diagram model:
```Java
Grammar grammar = new Grammar(rules);
GrammarToRRDiagram grammarToRRDiagram = new GrammarToRRDiagram();
for(Rule rule: grammar.getRules()) {
  RRDiagram rrDiagram = grammarToRRDiagram.convert(rule);
  // Do something with diagram, like get the SVG.
}
```

The grammar model can be created from code, or can read BNF syntax:
```Java
BNFToGrammar bnfToGrammar = new BNFToGrammar();
Grammar grammar = bnfToGrammar.convert(reader);
// Do something with grammar, like get the diagram for SVG output.
```

The grammar model can also be saved to BNF syntax:
```Java
GrammarToBNF grammarToBNF = new GrammarToBNF();
// Set options on the grammarToBNF object
String bnf = grammarToBNF.convert(grammar);
```

BNF Syntax
==========

The supported BNF subset when reading is the following:
<pre>
- definition
    =
    :=
    ::=
- concatenation
    ,
    &lt;whitespace&gt;
- termination
    ;
- alternation
    |
- option
    [ ... ]
    ?
- repetition
    { ... } =&gt; 0..N
    expression* =&gt; 0..N
    expression+ =&gt; 1..N
    &lt;digits&gt; * expression => &lt;digits&gt;...&lt;digits&gt;
    &lt;digits&gt; * [expression] => &lt;0&gt;...&lt;digits&gt;
    &lt;digits&gt; * expression? => &lt;0&gt;...&lt;digits&gt;
- grouping
    ( ... )
- literal
    " ... " or ' ... '
- special characters
    (? ... ?)
- comments
    (* ... *)
</pre>

When getting the BNF syntax from the grammar model, it is possible to tweak the kind of BNF to get by changing some options on the converter.

License
=======

This library is provided under the ASL 2.0.
