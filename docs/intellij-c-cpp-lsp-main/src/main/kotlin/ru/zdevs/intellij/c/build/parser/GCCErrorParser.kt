package ru.zdevs.intellij.c.build.parser

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.output.BuildOutputInstantReader
import ru.zdevs.intellij.c.build.MakeBuildParser
import java.util.function.Consumer
import java.util.regex.Matcher
import java.util.regex.Pattern

class GCCErrorParser(
    regexp: String,
    private val kind: MessageEvent.Kind,
    message: Int, file: Int, lineNumber: Int, linePosition: Int
) : FlexErrorParser(regexp, kind, message, file, lineNumber, linePosition) {

    override fun process(
        build: BaseBuildParser,
        line: String,
        message: String,
        filePosition: FilePosition,
        matcher: Matcher,
        reader: BuildOutputInstantReader,
        consumer: Consumer<in BuildEvent>
    ): FileMessageEventImpl? {
        if (filePosition.file.name.startsWith('<'))
            return null

        var processMessage: StringBuilder? = null

        do {
            val readLine = reader.readLine() ?: break

            val cleanReadLine = MakeBuildParser.removeEscapeSequences(readLine)
            val continueMatcher = errorContinueParser.matcher(cleanReadLine)
            if (!continueMatcher.find()) {
                break
            }

            if (processMessage == null) {
                processMessage = StringBuilder()
            }
            processMessage.append(readLine + "\n")
        } while (true)

        reader.pushBack(1)

        if (processMessage != null) {
            consumer.accept(OutputBuildEventImpl(build.getBuildId(), processMessage.toString(), true))
        }

        val detail = if (processMessage != null) {
            line + "\n" + processMessage.toString()
        } else {
            null
        }

        return FileMessageEventImpl(
            build.getBuildId(),
            kind,
            MakeBuildParser.C_MESSAGE_GROUP,
            message,
            detail,
            filePosition
        )
    }

    companion object {
        private val errorContinueParser = Pattern.compile("[ \\d]+\\|(.*)")
    }
}