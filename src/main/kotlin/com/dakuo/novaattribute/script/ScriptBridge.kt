package com.dakuo.novaattribute.script

import com.dakuo.novascript.NovaCompiled
import com.dakuo.novascript.NovaScriptAPI
import com.dakuo.novascript.RunMode
import com.dakuo.novascript.ScriptConfigurer
import java.io.File

object ScriptBridge {

    private const val PLUGIN_NAME = "NovaAttribute"

    fun register(name: String, file: File, scriptDir: File = file.parentFile, setup: ScriptConfigurer? = null): Boolean {
        val configurer = ScriptConfigurer { s ->
            ScriptBinding.configure(s)
            setup?.configure(s)
        }
        return NovaScriptAPI.register(PLUGIN_NAME, name, file, scriptDir, RunMode.BYTECODE, configurer, "novaattr")
    }

    fun callFunction(scriptName: String, funcName: String, vararg args: Any?): Any? {
        return NovaScriptAPI.callFunction(PLUGIN_NAME, scriptName, funcName, *args)
    }

    fun hasFunction(scriptName: String, funcName: String): Boolean {
        return NovaScriptAPI.hasFunction(PLUGIN_NAME, scriptName, funcName)
    }

    fun compile(code: String, setup: ScriptConfigurer? = null): NovaCompiled {
        val configurer = ScriptConfigurer { s ->
            ScriptBinding.configure(s)
            setup?.configure(s)
        }
        return NovaScriptAPI.compileToBytecode(code, configurer)
    }

    fun unregister(name: String) {
        NovaScriptAPI.unregister(PLUGIN_NAME, name)
    }

    fun reload(name: String): Boolean {
        return NovaScriptAPI.reload(PLUGIN_NAME, name)
    }

    fun unregisterAll() {
        NovaScriptAPI.unregisterAll(PLUGIN_NAME)
    }

    fun isLoaded(name: String): Boolean {
        return NovaScriptAPI.isLoaded(PLUGIN_NAME, name)
    }
}
