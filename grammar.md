```
expression     → literal
               | unary
               | binary
               | grouping ;

literal        → NUMBER | STRING | "true" | "false" | "nil" ;
grouping       → "(" expression ")" ;
unary          → ( "-" | "!" ) expression ;
binary         → expression operator expression ;
operator       → "==" | "!=" | "<" | "<=" | ">" | ">="
               | "+"  | "-"  | "*" | "/" ;
```
Right now, the grammar stuffs all expression types into a single expression rule. That same rule is used as the non-terminal for operands, which lets the grammar accept any kind of expression as a subexpression, regardless of whether the precedence rules allow it.

We fix that by stratifying the grammar. We define a separate rule for each precedence level.
```
expression     → ...
equality       → ...
comparison     → ...
term           → ...
factor         → ...
unary          → ...
primary        → ...
```

Each rule here only matches expressions at its precedence level or higher. For example, unary matches a unary expression like !negated or a primary expression like 1234. And term can match 1 + 2 but also 3 * 4 / 5. The final primary rule covers the highest-precedence forms—literals and parenthesized expressions.

We just need to fill in the productions for each of those rules. We’ll do the easy ones first. The top expression rule matches any expression at any precedence level. Since equality has the lowest precedence, if we match that, then it covers everything.

Instead of baking precedence right into the grammar rules, some parser generators let you keep the same ambiguous-but-simple grammar and then add in a little explicit operator precedence metadata on the side in order to disambiguate.

Also, in later chapters when we expand the grammar to include assignment and logical operators, we’ll only need to change the production for expression instead of touching every rule that contains an expression.
```
expression     → equality
```

We could eliminate expression and simply use equality in the other rules that contain expressions, but using expression makes those other rules read a little better.

Over at the other end of the precedence table, a primary expression contains all the literals and grouping expressions.
```
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;
```

A unary expression starts with a unary operator followed by the operand. Since unary operators can nest—!!true is a valid if weird expression—the operand can itself be a unary operator. A recursive rule handles that nicely.
```
unary          → ( "!" | "-" ) unary ;
```

But this rule has a problem. It never terminates.

Remember, each rule needs to match expressions at that precedence level or higher, so we also need to let this match a primary expression.
```
unary          → ( "!" | "-" ) unary
               | primary ;
```

That works.

The remaining rules are all binary operators. We’ll start with the rule for multiplication and division. Here’s a first try:
```
factor         → factor ( "/" | "*" ) unary
               | unary ;
```

The rule recurses to match the left operand. That enables the rule to match a series of multiplication and division expressions like 1 * 2 / 3. Putting the recursive production on the left side and unary on the right makes the rule left-associative and unambiguous.

In principle, it doesn’t matter whether you treat multiplication as left- or right-associative—you get the same result either way. Alas, in the real world with limited precision, roundoff and overflow mean that associativity can affect the result of a sequence of multiplications. Consider:
```
print 0.1 * (0.2 * 0.3);
print (0.1 * 0.2) * 0.3;
```

In languages like Lox that use IEEE 754 double-precision floating-point numbers, the first evaluates to 0.006, while the second yields 0.006000000000000001. Sometimes that tiny difference matters. This is a good place to learn more.

All of this is correct, but the fact that the first symbol in the body of the rule is the same as the head of the rule means this production is left-recursive. Some parsing techniques, including the one we’re going to use, have trouble with left recursion. (Recursion elsewhere, like we have in unary and the indirect recursion for grouping in primary are not a problem.)

There are many grammars you can define that match the same language. The choice for how to model a particular language is partially a matter of taste and partially a pragmatic one. This rule is correct, but not optimal for how we intend to parse it. Instead of a left recursive rule, we’ll use a different one.
```
factor         → unary ( ( "/" | "*" ) unary )* ;
```

We define a factor expression as a flat sequence of multiplications and divisions. This matches the same syntax as the previous rule, but better mirrors the code we’ll write to parse Lox. We use the same structure for all of the other binary operator precedence levels, giving us this complete expression grammar:
```
expression     → equality ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;
unary          → ( "!" | "-" ) unary
               | primary ;
primary        → NUMBER | STRING | "true" | "false" | "nil"
               | "(" expression ")" ;
```

This grammar is more complex than the one we had before, but in return we have eliminated the previous one’s ambiguity. It’s just what we need to make a parser.