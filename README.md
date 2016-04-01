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

   Representation            Important classes
   --------------            -----------------

      source code            String
          |
          o--- lexer         siilinkari.lexer.{Lexer,LookaheadLexer}
          |
          v
       tokens                siilinkari.lexer.Token
          |
          o--- parser        siilinkari.parser.Parser
          |
          v
  abstract syntax tree       siilinkari.ast.{Expression, Statement}
          |
          o--- evaluator     siilinkari.eval.{Evaluator, Environment}
          |
          v
        values               siilinkari.objects.Value

```

Start from the important classes and either work your way through the pipeline, or just concentrate
on a single transformation pass.
