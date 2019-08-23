RRDiagram
=========

Tool for generating railroad diagrams and doc-style grammar for [YugaByte API docs](docs.yugabyte.com).

Tool is based on: https://github.com/Chrriis/RRDiagram.

Example
=======

This is the kind of grammars and diagrams that can get generated:
https://docs.yugabyte.com/latest/api/ysql/commands/cmd_copy/

Usage
=======

1. Get the executable jar

```bash
 wget https://github.com/YugaByte/RRDiagram/releases/download/0.9.4/rrdiagram.jar
```
_Note: Alternatively build manually as described in the [Build](#build) section below (and move/rename the resulting jar from the target folder)._

1. Get fonts, if necessary.  At the time of writing, Yugabyte uses _Verdana_.

1. Run the diagram generator:

```bash
java -jar rrdiagram.jar <input-file.ebnf> <output-folder>
```
_Note: run `java -jar rrdiagram.jar` (without arguments) to see help._


For more detailed instructions see the [YugaByte Docs Readme](https://github.com/YugaByte/yugabyte-db/blob/master/docs/README.md).

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
