build: clean
	mvn package -DskipTests=true

clean:
	mvn clean

test:
	mvn test

server: build
	java -jar target/*.jar --server --ebnf ./ysql_grammar.ebnf --debug

package: build
	rm -f rrdiagram-*.jar
	cp target/rrdiagram-*.jar .
	npm pack --pack-destination target
	rm rrdiagram-*.jar

publish:
	cd target
	npm publish yb-rrdiagram*.tgz --access public

.PHONY: build clean test server package publish
