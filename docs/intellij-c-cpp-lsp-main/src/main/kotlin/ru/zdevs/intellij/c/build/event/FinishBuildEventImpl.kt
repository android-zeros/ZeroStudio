package ru.zdevs.intellij.c.build.event

import com.intellij.build.events.EventResult
import com.intellij.build.events.FinishBuildEvent
import com.intellij.build.events.impl.FinishEventImpl

class FinishBuildEventImpl(eventId: Any, parentId: Any?, eventTime: Long, message: String, result: EventResult) :
    FinishEventImpl(eventId, parentId, eventTime, message, result), FinishBuildEvent
