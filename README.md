ocamljava-maven-plugin
======================

This plugin provides maven integration for ocaml java. See http://www.ocamljava.org for details on that specific project. The important 
thing to know is it's a pure java solution to integrating ocaml code with the JVM, and this project provides the necessary maven integration.

Ocaml code can access compiled java sources from the working project, as well any of the maven dependencies listed in the project's pom file.

Conversely, java code can access ocaml code from other maven projects. Currently, it's not possible for java code to reference ocaml code
within the same project. When this is required you must factor out the relevant java code into a separate maven project.


