package ru.zdevs.intellij.c.build.parser

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.output.BuildOutputInstantReader
import ru.zdevs.intellij.c.build.MakeBuildParser
import java.util.function.Consumer
import java.util.regex.Matcher
import java.util.regex.Pattern

open class FlexErrorParser(
    regexp: String,
    private val kind: MessageEvent.Kind,
    private val message: Int, private val file: Int, private val lineNumber: Int, private val linePosition: Int
) : IErrorParser {

    private val pattern = Pattern.compile(regexp)

    protected open fun process(
        build: BaseBuildParser,
        line: String,
        message: String,
        filePosition: FilePosition,
        matcher: Matcher,
        reader: BuildOutputInstantReader,
        consumer: Consumer<in BuildEvent>
    ): FileMessageEventImpl? {
        return FileMessageEventImpl(
            build.getBuildId(),
            kind,
            MakeBuildParser.C_MESSAGE_GROUP,
            message,
            matcher.group(0),
            filePosition
        )
    }

    override fun parse(build: BaseBuildParser, line: String, lineRaw: String, reader: BuildOutputInstantReader, consumer: Consumer<in BuildEvent>): Boolean {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            val cnt = matcher.groupCount()

            val filePosition = if (file != 0 && file <= cnt) {
                FilePosition(
                    build.getFilePath(matcher.group(file)),
                    if (lineNumber != 0 && lineNumber <= cnt) (matcher.group(lineNumber).toIntOrNull() ?: 1) - 1 else 0,
                    if (linePosition != 0 && linePosition <= cnt) (matcher.group(linePosition).toIntOrNull() ?: 1) - 1 else 0
                )
            } else {
                FilePosition(build.getFilePath(""), 0, 0)
            }

            val event = process(
                build,
                lineRaw,
                if (message != 0 && message <= cnt) matcher.group(message) else "",
                filePosition,
                matcher,
                reader,
                consumer
            )
            if (event != null) {
                consumer.accept(event)
                build.addKind(kind)
                return true
            }
        }
        return false
    }
}