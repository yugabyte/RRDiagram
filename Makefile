
build:
	mvn package -DskipTests=true

server: build
	java -jar target/*.jar --server --bnf ./ysql_grammar.ebnf --debug

.PHONY: build docs server
