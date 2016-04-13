package siilinkari.repl

import jline.console.ConsoleReader
import siilinkari.lexer.SyntaxErrorException
import siilinkari.lexer.UnexpectedEndOfInputException
import siilinkari.runtime.registerRuntimeFunctions
import siilinkari.types.Type
import siilinkari.types.TypeCheckException
import siilinkari.vm.Evaluator
import kotlin.system.measureTimeMillis

/**
 * Implementation of Read-Eval-Print loop.
 */
fun main(args: Array<String>) {

    val console = ConsoleReader()
    console.expandEvents = false

    val evaluator = Evaluator()
    registerRuntimeFunctions(evaluator)

    console.addCompleter { s, i, result ->
        val word = s.subSequence(0, i).takeLastWhile { it.isJavaIdentifierPart() }

        for (binding in evaluator.bindingsNames())
            if (binding.startsWith(word, ignoreCase = true))
                result += binding

        i - word.length
    }

    evaluator.loadResource("prelude.sk")

    println("Welcome to Siilinkari! Enjoy your stay or type 'exit' to get out.")

    var showElapsedTime = false
    while (true) {
        var line = console.readLine(">>> ")?.trim() ?: break

        if (line == "") continue
        if (line == "exit") break

        if (line == ":trace") {
            evaluator.trace = !evaluator.trace;
            println("trace ${if (evaluator.trace) "on" else "off"}")
            continue
        } else if (line == ":time") {
            showElapsedTime = !showElapsedTime
            println("time ${if (showElapsedTime) "on" else "off"}")
            continue
        }

        try {
            if (line.startsWith(":dump ")) {
                println(evaluator.dump(line.substringAfter(":dump ")))
            } else {
                while (true) {
                    try {
                        val elapsedTime = measureTimeMillis {
                            val (value, type) = evaluator.evaluate(line)
                            if (type != Type.Unit)
                                println("$type = ${value.repr()}")
                        }

                        if (showElapsedTime) {
                            println("time: ${elapsedTime}ms")
                        }
                        break
                    } catch (e: UnexpectedEndOfInputException) {
                        val newLine = console.readLine("... ") ?: break
                        line = line + '\n' + newLine
                    }
                }
            }
        } catch (e: SyntaxErrorException) {
            println("Syntax error: ${e.errorMessage}")
            println(e.sourceLocation.toLongString())
        } catch (e: TypeCheckException) {
            println("Type checking failed: ${e.errorMessage}")
            println(e.sourceLocation.toLongString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    println("Thank you for visiting Siilinkari, have a nice day!")
}
