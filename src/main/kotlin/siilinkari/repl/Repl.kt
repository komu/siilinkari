package siilinkari.repl

import jline.console.ConsoleReader
import siilinkari.lexer.SyntaxErrorException
import siilinkari.objects.Value
import siilinkari.runtime.registerRuntimeFunctions
import siilinkari.types.TypeCheckException
import siilinkari.vm.Evaluator

/**
 * Implementation of Read-Eval-Print loop.
 */
fun main(args: Array<String>) {

    val console = ConsoleReader()

    val evaluator = Evaluator()
    registerRuntimeFunctions(evaluator)

    println("Welcome to Siilinkari! Enjoy your stay or type 'exit' to get out.")

    while (true) {
        var line = console.readLine("> ")?.trim() ?: break

        if (line == "") continue
        if (line == "exit") break

        if (!line.endsWith(';') && !line.endsWith("}"))
            line += ';'

        try {
            if (line.startsWith(":dump ")) {
                println(evaluator.dump(line.substringAfter(":dump ")))
            } else {
                val result = evaluator.evaluateStatement(line)
                if (result != Value.Unit)
                    println(result.repr())
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
