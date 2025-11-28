package ru.zdevs.intellij.c.build.event

import com.intellij.build.BuildDescriptor
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.events.impl.StartEventImpl

class StartBuildEventImpl(private val descriptor: DefaultBuildDescriptor, message: String) :
    StartEventImpl(descriptor.id, null, descriptor.startTime, message), StartBuildEvent {

    override fun getBuildDescriptor(): BuildDescriptor {
        return descriptor
    }
}
