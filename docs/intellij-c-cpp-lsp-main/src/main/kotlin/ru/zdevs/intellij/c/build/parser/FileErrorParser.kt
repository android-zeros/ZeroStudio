package ru.zdevs.intellij.c.build.parser

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.output.BuildOutputInstantReader
import ru.zdevs.intellij.c.build.MakeBuildParser
import java.util.function.Consumer
import java.util.regex.Pattern

class FileErrorParser(
    regexp: String,
    private val kind: MessageEvent.Kind,
    private val message: Int, private val file: String, private val lineNumber: Int, private val linePosition: Int
) : IErrorParser {

    private val pattern = Pattern.compile(regexp)

    override fun parse(build: BaseBuildParser, line: String, lineRaw: String, reader: BuildOutputInstantReader, consumer: Consumer<in BuildEvent>): Boolean {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            val cnt = matcher.groupCount()

            val filePosition = FilePosition(
                    build.getFilePath(file),
                    if (lineNumber != 0 && lineNumber <= cnt) (matcher.group(lineNumber).toIntOrNull() ?: 1) - 1 else 0,
                    if (linePosition != 0 && linePosition <= cnt) (matcher.group(linePosition).toIntOrNull() ?: 1) - 1 else 0
                )

            consumer.accept(FileMessageEventImpl(
                build.getBuildId(),
                kind,
                MakeBuildParser.C_MESSAGE_GROUP,
                if (message != 0 && message <= cnt) matcher.group(message) else "",
                matcher.group(0),
                filePosition
            ))
            build.addKind(kind)
            return true

        }
        return false
    }
}
