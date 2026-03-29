package com.dakuo.novaattribute.feature.storage

import com.dakuo.novaattribute.core.attribute.AttributeData
import com.dakuo.novaattribute.core.buff.Buff
import com.dakuo.rulib.common.database.Database
import com.dakuo.rulib.common.database.SQLTable
import com.dakuo.rulib.common.database.SQLTable.Companion.ColumnType
import com.dakuo.rulib.common.database.and
import com.dakuo.rulib.common.database.eq
import com.dakuo.rulib.common.database.gt
import com.dakuo.rulib.common.database.gte
import com.dakuo.rulib.common.database.lt
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Configuration
import java.util.UUID

/**
 * 数据持久化（DESIGN.md 4.26）
 * 使用 Rulib Database 模块实现 Buff/冷却/计数器的可选持久化
 */
object StorageManager {

    private var enabled = false
    private lateinit var buffTable: SQLTable
    private lateinit var cooldownTable: SQLTable
    private lateinit var counterTable: SQLTable

    fun init(config: Configuration) {
        val type = config.getString("database.type") ?: return
        if (type.isBlank()) return

        try {
            buffTable = Database.table("nova_buffs") {
                column("id") { type(ColumnType.Int); primaryKey(); autoIncrement() }
                column("uuid") { type(ColumnType.VarChar(36)); notNull() }
                column("buff_id") { type(ColumnType.VarChar(128)); notNull() }
                column("attr_data") { type(ColumnType.Text) }
                column("duration") { type(ColumnType.BigInt) }
                column("expire_at") { type(ColumnType.BigInt) }
                column("stacks") { type(ColumnType.Int); default("1") }
                column("stackable") { type(ColumnType.TinyInt); default("0") }
                uniqueIndex("idx_nova_buffs_uuid_bid", listOf("uuid", "buff_id"))
            }
            buffTable.create()

            cooldownTable = Database.table("nova_cooldowns") {
                column("id") { type(ColumnType.Int); primaryKey(); autoIncrement() }
                column("uuid") { type(ColumnType.VarChar(36)); notNull() }
                column("key_name") { type(ColumnType.VarChar(128)); notNull() }
                column("expire_at") { type(ColumnType.BigInt) }
                uniqueIndex("idx_nova_cd_uuid_key", listOf("uuid", "key_name"))
            }
            cooldownTable.create()

            counterTable = Database.table("nova_counters") {
                column("id") { type(ColumnType.Int); primaryKey(); autoIncrement() }
                column("uuid") { type(ColumnType.VarChar(36)); notNull() }
                column("key_name") { type(ColumnType.VarChar(128)); notNull() }
                column("value") { type(ColumnType.Int); default("0") }
                uniqueIndex("idx_nova_cnt_uuid_key", listOf("uuid", "key_name"))
            }
            counterTable.create()
            enabled = true
        } catch (e: Exception) {
            warning("[NovaAttribute] Failed to init storage: ${e.message}")
        }
    }

    fun isEnabled(): Boolean = enabled

    // ====== Buff 持久化 ======

    fun saveBuffs(uuid: UUID, buffs: List<Buff>) {
        if (!enabled) return
        try {
            val uuidStr = uuid.toString()
            buffTable.batchInsertOrUpdate(buffs.map { buff ->
                mapOf(
                    "uuid" to uuidStr,
                    "buff_id" to buff.id,
                    "attr_data" to serializeData(buff.data),
                    "duration" to buff.duration,
                    "expire_at" to buff.expireAt,
                    "stacks" to buff.stacks,
                    "stackable" to if (buff.stackable) 1 else 0
                )
            }, keys = listOf("uuid", "buff_id"))
        } catch (e: Exception) {
            warning("[NovaAttribute] Failed to save buffs for $uuid: ${e.message}")
        }
    }

    fun loadBuffs(uuid: UUID): List<Buff> {
        if (!enabled) return emptyList()
        try {
            val uuidStr = uuid.toString()
            val result = buffTable.query({ rs ->
                Buff(
                    id = rs.getString("buff_id"),
                    data = deserializeData(rs.getString("attr_data") ?: ""),
                    duration = rs.getLong("duration"),
                    expireAt = rs.getLong("expire_at"),
                    stacks = rs.getInt("stacks"),
                    stackable = rs.getInt("stackable") == 1,
                    persistent = true
                )
            }) {
                where("uuid" eq uuidStr)
            }
            val valid = result.filter { !it.isExpired() }
            if (valid.size < result.size) {
                buffTable.delete {
                    where(("uuid" eq uuidStr) and ("duration" gte 0) and ("expire_at" lt System.currentTimeMillis()))
                }
            }
            return valid
        } catch (e: Exception) {
            warning("[NovaAttribute] Failed to load buffs for $uuid: ${e.message}")
            return emptyList()
        }
    }

    // ====== Cooldown 持久化 ======

    fun saveCooldowns(uuid: UUID, cooldowns: Map<String, Long>) {
        if (!enabled) return
        try {
            val uuidStr = uuid.toString()
            val now = System.currentTimeMillis()
            val validEntries = cooldowns.filter { it.value > now }
            cooldownTable.batchInsertOrUpdate(validEntries.map { (key, expireAt) ->
                mapOf(
                    "uuid" to uuidStr,
                    "key_name" to key,
                    "expire_at" to expireAt
                )
            }, keys = listOf("uuid", "key_name"))
        } catch (e: Exception) {
            warning("[NovaAttribute] Failed to save cooldowns for $uuid: ${e.message}")
        }
    }

    fun loadCooldowns(uuid: UUID): Map<String, Long> {
        if (!enabled) return emptyMap()
        try {
            return cooldownTable.query({ rs ->
                rs.getString("key_name") to rs.getLong("expire_at")
            }) {
                where(("uuid" eq uuid.toString()) and ("expire_at" gt System.currentTimeMillis()))
            }.toMap()
        } catch (e: Exception) {
            warning("[NovaAttribute] Failed to load cooldowns for $uuid: ${e.message}")
            return emptyMap()
        }
    }

    // ====== Counter 持久化 ======

    fun saveCounters(uuid: UUID, counters: Map<String, Int>) {
        if (!enabled) return
        try {
            val uuidStr = uuid.toString()
            counterTable.batchInsertOrUpdate(counters.map { (key, value) ->
                mapOf(
                    "uuid" to uuidStr,
                    "key_name" to key,
                    "value" to value
                )
            }, keys = listOf("uuid", "key_name"))
        } catch (e: Exception) {
            warning("[NovaAttribute] Failed to save counters for $uuid: ${e.message}")
        }
    }

    fun loadCounters(uuid: UUID): Map<String, Int> {
        if (!enabled) return emptyMap()
        try {
            return counterTable.query({ rs ->
                rs.getString("key_name") to rs.getInt("value")
            }) {
                where("uuid" eq uuid.toString())
            }.toMap()
        } catch (e: Exception) {
            warning("[NovaAttribute] Failed to load counters for $uuid: ${e.message}")
            return emptyMap()
        }
    }

    // ====== 序列化 ======

    private fun serializeData(data: AttributeData): String {
        return data.toMap().entries.joinToString(";") { (attrId, values) ->
            "$attrId=${values.joinToString(",")}"
        }
    }

    private fun deserializeData(str: String): AttributeData {
        val data = AttributeData()
        if (str.isBlank()) return data
        for (entry in str.split(";")) {
            val parts = entry.split("=", limit = 2)
            if (parts.size != 2) continue
            val attrId = parts[0]
            val values = parts[1].split(",").mapNotNull { it.toDoubleOrNull() }
            if (values.isNotEmpty()) data.set(attrId, values)
        }
        return data
    }
}
