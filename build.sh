#!/bin/sh
javac com/craftinginterpreters/tool/*.java
java -classpath . com.craftinginterpreters.tool.GenerateAST com/craftinginterpreters/lox/ 
javac com/craftinginterpreters/lox/*.java
