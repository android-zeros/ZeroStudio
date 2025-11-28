package ru.zdevs.intellij.c.build.parser

import com.intellij.build.events.BuildEvent
import com.intellij.build.output.BuildOutputInstantReader
import java.util.function.Consumer
import java.util.regex.Matcher
import java.util.regex.Pattern

class CallbackErrorParser(
    regexp: String,
    private val callback: (build: BaseBuildParser, matcher: Matcher) -> Unit
) : IErrorParser {

    private val pattern = Pattern.compile(regexp)

    override fun parse(build: BaseBuildParser, line: String, lineRaw: String, reader: BuildOutputInstantReader, consumer: Consumer<in BuildEvent>): Boolean {
        val matcher = pattern.matcher(line)
        if (matcher.find()) {
            callback(build, matcher)
            return true
        }
        return false
    }
}