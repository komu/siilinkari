package siilinkari.repl

import siilinkari.eval.EvaluationException
import siilinkari.eval.Evaluator
import siilinkari.lexer.SyntaxErrorException
import java.util.*

fun main(args: Array<String>) {

    val scanner = Scanner(System.`in`)

    val evaluator = Evaluator()

    println("Welcome to Siilinkari! Enjoy your stay or type 'exit' to get out.")

    while (true) {
        var line = scanner.nextLineWithPrompt("> ")?.trim() ?: break

        if (line == "") continue
        if (line == "exit") break

        if (!line.endsWith(';'))
            line += ';'

        try {
            val result = evaluator.evaluateStatement(line)
            if (result != null && result != Unit)
                println(result)
        } catch (e: SyntaxErrorException) {
            println("Syntax error: ${e.errorMessage}")
            println(e.sourceLocation.toLongString())
        } catch (e: EvaluationException) {
            println("Evaluation failed: ${e.errorMessage}")
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
