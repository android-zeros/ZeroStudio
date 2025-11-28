package ru.zdevs.intellij.c.build.parser

import com.intellij.build.events.BuildEvent
import com.intellij.build.output.BuildOutputInstantReader
import java.util.function.Consumer

fun interface IErrorParser {
    fun parse(build: BaseBuildParser, line: String, lineRaw: String, reader: BuildOutputInstantReader, consumer: Consumer<in BuildEvent>): Boolean
}