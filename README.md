# RRDiagram

Tool for generating railroad diagrams and doc-style grammar for [Yugabyte API docs](https://docs.yugabyte.com).

Tool is based on: <https://github.com/Chrriis/RRDiagram>.

## Example

This is the kind of grammars and diagrams that can get generated:
<https://docs.yugabyte.com/latest/api/ysql/commands/cmd_copy/>

## Usage

See the [Yugabyte docs contributors guide](https://docs.yugabyte.com/preview/contribute/docs/syntax-diagrams/).

## Build

```bash
mvn package -DskipTests=true
```

## Internals

The diagram model represents the actual constructs visible on the diagram.
To convert a diagram model to SVG:

```java
RRDiagram rrDiagram = new RRDiagram(rrElement);
RRDiagramToSVG rrDiagramToSVG = new RRDiagramToSVG();
String svg = rrDiagramToSVG.convert(rrDiagram);
```

The grammar model represents a BNF-like grammar.
It can be converted to a diagram model:

```java
Grammar grammar = new Grammar(rules);
GrammarToRRDiagram grammarToRRDiagram = new GrammarToRRDiagram();
for(Rule rule: grammar.getRules()) {
  RRDiagram rrDiagram = grammarToRRDiagram.convert(rule);
  // Do something with diagram, like get the SVG.
}
```

The grammar model can be created from code, or can read BNF syntax:

```java
BNFToGrammar bnfToGrammar = new BNFToGrammar();
Grammar grammar = bnfToGrammar.convert(reader);
// Do something with grammar, like get the diagram for SVG output.
```

The grammar model can also be saved to BNF syntax:

```java
GrammarToBNF grammarToBNF = new GrammarToBNF();
// Set options on the grammarToBNF object
String bnf = grammarToBNF.convert(grammar);
```

## BNF Syntax

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

## License

This library is provided under the ASL 2.0.
