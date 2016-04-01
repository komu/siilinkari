package siilinkari.repl

import siilinkari.lexer.SyntaxErrorException
import siilinkari.types.TypeCheckException
import siilinkari.vm.Evaluator
import java.util.*

fun main(args: Array<String>) {

    val scanner = Scanner(System.`in`)
    val evaluator = Evaluator()

    println("Welcome to Siilinkari! Enjoy your stay or type 'exit' to get out.")

    while (true) {
        var line = scanner.nextLineWithPrompt("> ")?.trim() ?: break

        if (line == "") continue
        if (line == "exit") break

        if (!line.endsWith(';') && !line.endsWith("}"))
            line += ';'

        try {
            val result = evaluator.evaluateStatement(line)
            if (result != null)
                println(result)
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

fun Scanner.nextLineWithPrompt(prompt: String): String? {
    print(prompt)

    return if (hasNextLine()) nextLine() else null
}
