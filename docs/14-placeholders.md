# PlaceholderAPI 占位符

需要安装 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) 插件。NovaAttribute 启动时自动注册，无需手动安装扩展。

## 占位符列表

### 属性查询

| 占位符 | 说明 | 示例 |
|--------|------|------|
| `%novaattribute_<属性ID>%` | 属性当前值 | `%novaattribute_physical_damage%` → `100` |
| `%novaattribute_min_<属性ID>%` | 范围属性最小值 | `%novaattribute_min_physical_damage%` → `80` |
| `%novaattribute_max_<属性ID>%` | 范围属性最大值 | `%novaattribute_max_physical_damage%` → `120` |
| `%novaattribute_base_<属性ID>%` | 基础值（不含 Buff/公式） | `%novaattribute_base_max_health%` → `20` |

百分比属性（如暴击几率）自动格式化为 `25.0%`。

### 战斗力

| 占位符 | 说明 |
|--------|------|
| `%novaattribute_combat_power%` | 综合战斗力 |

### Buff

| 占位符 | 说明 |
|--------|------|
| `%novaattribute_buff_count%` | 当前 Buff 数量 |
| `%novaattribute_buff_list%` | Buff ID 列表（逗号分隔） |
| `%novaattribute_buff_<id>_remaining%` | 指定 Buff 剩余时间（秒） |
| `%novaattribute_buff_<id>_stacks%` | 指定 Buff 叠加层数 |

### 计数器

| 占位符 | 说明 |
|--------|------|
| `%novaattribute_counter_<key>%` | 计数器当前值 |

### 其他

| 占位符 | 说明 |
|--------|------|
| `%novaattribute_source_count%` | 属性来源数量 |

## 使用示例

### 记分板

```
&c❤ %novaattribute_max_health%
&6⚔ %novaattribute_physical_damage%
&e⭐ 战力 %novaattribute_combat_power%
```

### 聊天格式

```
[Lv.%player_level%] [战力:%novaattribute_combat_power%] %player_name%
```

### Tab 列表

```
%novaattribute_physical_damage% ATK | %novaattribute_physical_defense% DEF
```

### 条件插件（如 ConditionalEvents）

```yaml
conditions:
  high_damage:
    type: placeholder
    placeholder: "%novaattribute_physical_damage%"
    operator: ">="
    value: 1000
```
