# RRDiagram

Tool for generating railroad diagrams and doc-style grammar for [Yugabyte API docs](https://docs.yugabyte.com).

Tool is based on: <https://github.com/Chrriis/RRDiagram>.

## Example

This is the kind of grammar and syntax diagrams that can get generated: <https://docs.yugabyte.com/preview/api/ysql/commands/cmd_copy/>

## Usage

See the [Yugabyte docs contributors guide](https://docs.yugabyte.com/preview/contribute/docs/syntax-diagrams/).

## Running as a server

To start the diagram server, run `make server.` The service runs on port `1314` @ `/ebnf`. The service can be reached as `curl "localhost:1314/ebnf?mode=diagram&rules=select"`. These are the parameters supported.

| Argument | Description |
| -------- | ----------- |
| api      |  Language API name. One of - ysql, ycql. Support for ycql will be added soon (Default: ysql) |
| version  | YB Release version. One of - preview, stable, v2.12, v2.14, v2.16, v2.8. This ensures that the correct EBNF file is looked up for generating Grammar/Diagrams for a specific version. (Default: preview) |
| mode     | Method to execute. One of - reference, grammar, diagram (Mandatory)<br>- reference: Will return the grammar and diagrams for all the rules in the EBNF (Typically used to generate the reference file, i.e. grammar_diagrams.md)<br>- grammar: Will return diagram definitions in plain text <br>- diagram: Will return the diagram in SVG format |
| depth    | The depth (wrt to the doc root) at which the generated diagrams/definition will be embedded. This is used to generate the correct relative link to the reference file. (Optional) |
| rules    | Comma-separated names of rules for which syntax diagrams or diagram definitions need to be generated (eg: rules=select,select_start). (Mandatory for grammar and diagram modes) |
| local    | Comma-separated names of rules for which the definition will be xref'ed to the same page instead of pointing to the definition in the reference file. This is to be used when multiple grammar/diagram tabs are embedded on the same page and one diagram refers to the other. (eg: local=select_options). Invalid rules are ignored. |

## Build

```bash
make
```

## Publishing to the npm registry

1. Update the version number in [package.json](package.json)
1. Build the npm package using `make package`
1. You need an account on npm to publish. If you don't have one, go to [www.npmjs.com](https://www.npmjs.com/) and create a free account.
1. Authenticate on the command line with `npm login`
1. Publish using `make publish`

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
