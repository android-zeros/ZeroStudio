package ru.zdevs.intellij.c.build

import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import com.intellij.openapi.util.text.StringUtil
import org.intellij.markdown.lexer.Stack
import ru.zdevs.intellij.c.build.parser.*
import java.io.File
import java.util.function.Consumer
import java.util.regex.Matcher

class MakeBuildParser(ctx: CBuildContext, private val path: String?) : BuildOutputParser, BaseBuildParser(ctx) {
    private val workingDirection: Stack<String> = Stack()

    private val parsers: Array<IErrorParser> = arrayOf(
        // GCC / Clang
        GCCErrorParser("(.*?):(\\d+):((\\d+):)?\\s*(fatal )?(([Ee]rror)|(ERROR)): (.*)", MessageEvent.Kind.ERROR, 9, 1, 2, 4),
        GCCErrorParser("(.*?):(\\d+):((\\d+):)?\\s*(([Ww]arning)|(WARNING)): (.*)", MessageEvent.Kind.WARNING, 8, 1, 2, 4),
        GCCErrorParser("(.*?):(\\d+):((\\d+):)?\\s*(([Nn]ote)|(NOTE)|([Ii]nfo)|(INFO)|(remark)): (.*)", MessageEvent.Kind.INFO, 11, 1, 2, 4),
        // GCC / Clang - LD
        FlexErrorParser("(.*[/\\\\])?(ld|gcc|clang)(\\.exe)?: [Ee]rror:? (.*)", MessageEvent.Kind.ERROR, 3, 1, 0, 0),
        FlexErrorParser("(.*[/\\\\])?(ld|gcc|clang)(\\.exe)?: [Ww]arning:? (.*)", MessageEvent.Kind.WARNING, 3, 1, 0, 0),
        // make
        FlexErrorParser("make(\\[\\d+\\])?: \\*\\*\\* \\[(.*?):(\\d+):[^\\]]*\\] (.*)", MessageEvent.Kind.ERROR, 4, 2, 3, 0),
        FileErrorParser("make(\\[\\d+\\])?: \\*\\*\\* (.*)", MessageEvent.Kind.ERROR, 2, "Makefile", 0, 0),
        FileErrorParser("Makefile:(\\d+): warning: (.*)", MessageEvent.Kind.WARNING, 2, "Makefile", 1, 0),
        CallbackErrorParser("make\\[(\\d+)\\]: Entering directory '(.*)'") { _: BaseBuildParser, matcher: Matcher ->
            val level = matcher.group(1).toIntOrNull() ?: 0
            var parseLevel: Int = workingDirection.size
            while (level < parseLevel) {
                workingDirection.pop()
                parseLevel--
            }
            workingDirection.push(matcher.group(2))
        },
        CallbackErrorParser("make\\[(\\d+)\\]: Leaving directory '(.*)'") { _, _ ->
            workingDirection.pop()
        },
    )

    override fun parse(
        line: String,
        reader: BuildOutputInstantReader,
        messageConsumer: Consumer<in BuildEvent>,
    ): Boolean {
        messageConsumer.accept(OutputBuildEventImpl(getBuildId(), withNewLine(line), true))

        val cleanLine = removeEscapeSequences(line)
        for (p in parsers) {
            if (p.parse(this, cleanLine, line, reader, messageConsumer)) {
                break
            }
        }

        return true
    }

    private fun getWorkingDir(): String {
        return if (workingDirection.isEmpty())
            path ?: "/"
        else
            workingDirection.peek()
    }

    override fun getFilePath(file: String): File {
        return if (file.startsWith("/"))
            File(file)
        else
            File(getWorkingDir(), file)
    }

    companion object {
        const val C_MESSAGE_GROUP: String = "C/C++ Builder"

        fun withNewLine(str: String): String = if (StringUtil.endsWithLineBreak(str)) str else str + '\n'

        fun removeEscapeSequences(text: String): String {
            return text.replace(Regex("\u001B\\[[;\\d]*m"), "")
        }
    }
}