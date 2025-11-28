package ru.zdevs.intellij.c.build

import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import ru.zdevs.intellij.c.build.parser.*
import java.io.File
import java.util.function.Consumer

class CMakeBuildParser(ctx: CBuildContext, private val path: String?) : BuildOutputParser, BaseBuildParser(ctx) {
    private val parsers: Array<IErrorParser> = arrayOf(
        FlexErrorParser("CMake Error at (.*?):(\\d+) (.*?)", MessageEvent.Kind.ERROR, 3, 1, 2, 0),
        FlexErrorParser("CMake Error: (.*?)", MessageEvent.Kind.ERROR, 1, 0, 0, 0),
    )

    override fun parse(
        line: String,
        reader: BuildOutputInstantReader,
        messageConsumer: Consumer<in BuildEvent>,
    ): Boolean {
        messageConsumer.accept(OutputBuildEventImpl(getBuildId(), MakeBuildParser.withNewLine(line), true))

        val cleanLine = MakeBuildParser.removeEscapeSequences(line)
        for (p in parsers) {
            if (p.parse(this, cleanLine, line, reader, messageConsumer)) {
                break
            }
        }

        return true
    }

    override fun getFilePath(file: String): File {
        return if (file.startsWith("/"))
            File(file)
        else
            File(path ?: "/", file)
    }
}