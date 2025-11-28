package ru.zdevs.intellij.c.debug

import com.intellij.openapi.components.StoredProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.execution.ParametersListUtil
import com.redhat.devtools.lsp4ij.dap.configurations.DAPRunConfigurationOptions

class CRunConfigurationOptions : DAPRunConfigurationOptions() {
    private val stopAtMainProp: StoredProperty<Boolean> = property(true).provideDelegate(this, "stopAtMain")
    private val launchArgumentsProp: StoredProperty<String?> = string(null).provideDelegate(this, "launchArguments")

    var stopAtMain: Boolean
        get() = stopAtMainProp.getValue(this)
        set(value) = stopAtMainProp.setValue(this, value)

    var launchArguments: String?
        get() = launchArgumentsProp.getValue(this)
        set(value) = launchArgumentsProp.setValue(this, value)

    fun getLaunchArgumentJSONList() : List<String> {
        val launchArguments = launchArguments
        if (launchArguments != null) {
            val args = ParametersListUtil.parse(launchArguments)
            val list = ArrayList<String>(args.size)
            for (arg in args) {
                list.add("\"" + arg.replace("\"", "\\\"") + "\"")
            }
            return list
        } else {
            return listOf()
        }
    }

    override fun isDebuggableFile(file: VirtualFile, project: Project): Boolean {
        return MAPPING_FILE_TYPE.contains(file.extension)
    }

    companion object {
        private val MAPPING_FILE_TYPE = listOf("c", "cpp", "cc", "h", "hpp", "s", "S")
    }
}