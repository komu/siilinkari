# Siilinkari - a small language

Big languages (Java, Kotlin, Ceylon) seem to be named after big islands. Siilinkari is barely
a language, just like [Siilinkari](https://goo.gl/maps/zg4SnA5Ydym) is barely an island.

In fact, the point of Siilinkari is not to write programs in it, but rather to function as a readable
introduction to the world of compilers. You can browse the code, try out implementing new features
and play around.

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
> if (x == 2 + 2) { var t = "It"; s = t + " worked!"; }
> s
"It worked!"
> :dump if (x == 2 + 2) { var t = "It"; s = t + " worked!"; }
0 Load [Global x]
1 Push 2
2 Push 2
3 Add
4 Equal
5 JumpIfFalse [Label 8]
6 Push "It worked!"
7 Store [Global s]
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
          o--------- type checker  siilinkari.types.{Type, TypeChecker}, siilinkari.env.{StaticEnvironment, Binding}
          |
          v
    typed syntax tree              siilinkari.types.{TypedExpression, TypedStatement}
          |
          |
          o--------- translator    siilinkari.translator.Translator
          |
          v
    stack vm opcodes               siilinkari.vm.{OpCode, CodeSegment}
          |
          |
          o--------- evaluator     siilinkari.vm.{Evaluator, Environment}
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
- Add a simple evaluator that evaluates either the typed AST directly instead of translating the code
  to stack vm opcodes.
- Implement a pretty printer: it should take an AST and write it to output properly formatted.
- Modify lexer and parser so that semicolons are not required at the end of statements.
- Modify lexer and parser to support indentation based syntax.
- Add AST -> AST rewrite step that does constant folding before evaluation starts.
- Add support for higher order functions and lambdas.
- Add peephole optimizer for generated opcodes.

## Want more?

If you want a taste of a bigger language, take a look at [Blunt](https://bitbucket.org/komu/blunt).
It implements a large subset of ML/Haskell-like language (see e.g. [prelude.blunt](https://bitbucket.org/komu/blunt/src/f8a14979a743c4f06c85cffeee876111f2ac91ab/src/main/resources/prelude.blunt?at=master&fileviewer=file-view-default)
and [river.blunt](https://bitbucket.org/komu/blunt/src/f8a14979a743c4f06c85cffeee876111f2ac91ab/src/main/resources/river.blunt?at=master&fileviewer=file-view-default)), but naturally the implementation is a lot
more complex.
