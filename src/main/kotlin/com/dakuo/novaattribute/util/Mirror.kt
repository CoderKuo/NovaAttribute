package com.dakuo.novaattribute.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 轻量级性能分析器
 *
 * 使用方式:
 *   Mirror.time("refresh") { ... }
 *   Mirror.time("damage:calculate") { ... }
 *
 * 命令: /novas mirror — 打印性能报告
 */
object Mirror {

    val data = ConcurrentHashMap<String, MirrorData>()

    class MirrorData {
        val count = AtomicLong(0)
        val totalNs = AtomicLong(0)
        @Volatile var minNs = Long.MAX_VALUE
        @Volatile var maxNs = 0L

        fun record(ns: Long) {
            count.incrementAndGet()
            totalNs.addAndGet(ns)
            if (ns < minNs) minNs = ns
            if (ns > maxNs) maxNs = ns
        }

        fun avgMs(): Double = if (count.get() == 0L) 0.0 else (totalNs.get().toDouble() / count.get()) / 1_000_000.0
        fun minMs(): Double = if (minNs == Long.MAX_VALUE) 0.0 else minNs / 1_000_000.0
        fun maxMs(): Double = maxNs / 1_000_000.0
        fun totalMs(): Double = totalNs.get() / 1_000_000.0
    }

    /**
     * 计时执行代码块
     */
    inline fun <T> time(id: String, block: () -> T): T {
        val start = System.nanoTime()
        return block().also {
            val elapsed = System.nanoTime() - start
            data.getOrPut(id) { MirrorData() }.record(elapsed)
        }
    }

    /**
     * 生成性能报告
     */
    fun report(): List<String> {
        if (data.isEmpty()) {
            return listOf("§7没有性能数据。")
        }

        val totalAll = data.values.sumOf { it.totalNs.get() }.toDouble()
        val lines = mutableListOf<String>()
        lines.add("§6=== NovaAttribute Mirror 性能报告 ===")

        // 按总耗时降序排列
        val sorted = data.entries.sortedByDescending { it.value.totalNs.get() }
        for ((id, d) in sorted) {
            val percent = if (totalAll > 0) (d.totalNs.get() / totalAll * 100) else 0.0
            lines.add(String.format(
                "§e%-30s §f%6d次  §7avg=§f%.3fms  §7min=§f%.3fms  §7max=§f%.3fms  §7(%.1f%%)",
                id, d.count.get(), d.avgMs(), d.minMs(), d.maxMs(), percent
            ))
        }
        lines.add(String.format("§7总计: §f%.1fms", totalAll / 1_000_000.0))
        return lines
    }

    /**
     * 清空数据
     */
    fun clear() {
        data.clear()
    }
}
