import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import io.izzel.taboolib.gradle.Basic
import io.izzel.taboolib.gradle.BukkitFakeOp
import io.izzel.taboolib.gradle.BukkitHook
import io.izzel.taboolib.gradle.BukkitNMS
import io.izzel.taboolib.gradle.BukkitNMSDataSerializer
import io.izzel.taboolib.gradle.BukkitNMSItemTag
import io.izzel.taboolib.gradle.BukkitNMSUtil
import io.izzel.taboolib.gradle.BukkitUI
import io.izzel.taboolib.gradle.BukkitUtil
import io.izzel.taboolib.gradle.Database
import io.izzel.taboolib.gradle.LettuceRedis
import io.izzel.taboolib.gradle.CommandHelper
import io.izzel.taboolib.gradle.I18n
import io.izzel.taboolib.gradle.Metrics
import io.izzel.taboolib.gradle.MinecraftChat
import io.izzel.taboolib.gradle.MinecraftEffect
import io.izzel.taboolib.gradle.Bukkit


plugins {
    java
    id("io.izzel.taboolib") version "2.0.36"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

taboolib {
    env {
        install(Basic)
        install(BukkitFakeOp)
        install(BukkitHook)
        install(BukkitNMS)
        install(BukkitNMSDataSerializer)
        install(BukkitNMSItemTag)
        install(BukkitNMSUtil)
        install(BukkitUI)
        install(BukkitUtil)
        install(Database)
        install(LettuceRedis)
        install(CommandHelper)
        install(I18n)
        install(Metrics)
        install(MinecraftChat)
        install(MinecraftEffect)
        install(Bukkit)
    }
    description {
        name = "NovaAttribute"
        contributors {
            name("dakuo")
        }
        dependencies {
            name("NovaScript")
        }
    }
    version { taboolib = "6.2.4-99fb800" }


    relocate("com.dakuo.rulib.","com.dakuo.novaattribute.lib.")
    relocate("ink.ptms.um.", "com.dakuo.novaattribute.lib.um.")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("ink.ptms.core:v11200:11200")
    compileOnly(kotlin("stdlib"))

    taboo("com.github.CoderKuo:Rulib:v1.0.0")
    compileOnly("com.github.CoderKuo:NovaScript:v1.0.2")
    taboo("ink.ptms:um:1.2.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JVM_1_8)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}