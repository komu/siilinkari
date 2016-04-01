# Siilinkari - a small language

Big languages (Java, Kotlin, Ceylon) seem to be named after big islands. Siilinkari is barely
a language, just like [Siilinkari](https://goo.gl/maps/zg4SnA5Ydym) is barely an island.

In fact, the point of Siilinkari is not to write programs in it, but rather to function as a readable
introduction to the world of compilers. You can browse the code, try out implementing new features
and play around.

Siilinkari is a very small subset of Kotlin, so every program written in Siilinkari should be a valid
Kotlin program with same semantics. (But most Kotlin programs will not be supported by Siilinkari.)

## Build

```
# ./gradlew clean build
```

## Play with REPL

```
# java -jar build/libs/siilinkari-all.jar
Welcome to Siilinkari! Enjoy your stay or type 'exit' to get out.
> var x = 4;
> var s = "";
> if (x == 2 + 2) s = "It worked!";
> s
"It worked!"
> exit
Thank you for visiting Siilinkari, have a nice day!
```

## Get to know the code

The evaluator consists of the following pipeline:

```

   Representation    Stage        Important classes
   --------------    -----        -----------------

      source code                  String
          |
          |
          o--------- lexer         siilinkari.lexer.{Lexer,LookaheadLexer}
          |
          v
       tokens                      siilinkari.lexer.Token
          |
          |
          o--------- parser        siilinkari.parser.Parser
          |
          v
  abstract syntax tree             siilinkari.ast.{Expression, Statement}
          |
          |
          o--------- type checker  siilinkari.eval.{Type, TypeChecker, TypeEnvironment}
          |
          v
    typed syntax tree              siilinkari.types.{TypedExpression, TypedStatement}
          |
          |
          o--------- evaluator     siilinkari.eval.{Evaluator, Environment}
          |
          v
        values                     siilinkari.objects.Value

```

Start from the important classes and either work your way through the pipeline, or just concentrate
on a single transformation pass.

## Project ideas

If you want to play around, here are some ideas to get started:

- Add new `unless (x) { ... }` statement that works like `if (!x) { ... }`.
- Convert `if`-statement to expression so you can say `var x = if (a) b else c;`.
- Implement a pretty printer: it should take an AST and write it to output properly formatted.
- Modify lexer and parser so that semicolons are not required at the end of statements.
- Modify lexer and parser to support indentation based syntax.
- Add AST -> AST rewrite step that does constant folding before evaluation starts.
- Add support for higher order functions and lambdas.

## Want more?

If you want a taste of a bigger language, take a look at [Blunt](https://bitbucket.org/komu/blunt).
It implements a large subset of ML/Haskell-like language (see e.g. [prelude.blunt](https://bitbucket.org/komu/blunt/src/f8a14979a743c4f06c85cffeee876111f2ac91ab/src/main/resources/prelude.blunt?at=master&fileviewer=file-view-default)
and [river.blunt](https://bitbucket.org/komu/blunt/src/f8a14979a743c4f06c85cffeee876111f2ac91ab/src/main/resources/river.blunt?at=master&fileviewer=file-view-default)), but naturally the implementation is a lot
more complex.
