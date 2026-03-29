package com.dakuo.novaattribute.script

import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import java.io.File

object ScriptManager {

    private lateinit var scriptsDir: File

    fun init(dataFolder: File) {
        scriptsDir = File(dataFolder, "scripts")
        if (!scriptsDir.exists()) scriptsDir.mkdirs()
    }

    fun loadAll() {
        var count = 0
        // 加载 scripts/ 目录
        val files = scriptsDir.listFiles { f -> f.extension == "nova" } ?: emptyArray()
        for (file in files) {
            val name = file.nameWithoutExtension
            if (ScriptBridge.register(name, file, scriptsDir)) {
                count++
            } else {
                warning("[NovaAttribute] Failed to load script: ${file.name}")
            }
        }
        // 加载 conditions/ 目录（脚本名添加 condition_ 前缀）
        val condDir = File(scriptsDir.parentFile, "conditions")
        if (condDir.exists()) {
            val condFiles = condDir.listFiles { f -> f.extension == "nova" } ?: emptyArray()
            for (file in condFiles) {
                val name = "condition_${file.nameWithoutExtension}"
                if (ScriptBridge.register(name, file, condDir)) {
                    count++
                } else {
                    warning("[NovaAttribute] Failed to load script: conditions/${file.name}")
                }
            }
        }
        info("[NovaAttribute] Loaded $count scripts.")
    }

    fun load(name: String): Boolean {
        val file = File(scriptsDir, "$name.nova")
        if (!file.exists()) {
            warning("[NovaAttribute] Script not found: $name.nova")
            return false
        }
        return ScriptBridge.register(name, file, scriptsDir)
    }

    fun unload(name: String) {
        ScriptBridge.unregister(name)
    }

    fun reload() {
        ScriptBridge.unregisterAll()
        loadAll()
    }

    fun getScriptsDir(): File = scriptsDir
}
