# 命令参考

主命令：`/novaattribute`，别名：`/na`、`/novaa`

权限：`novaattribute.admin`

## 命令列表

### /na reload

重载配置文件、属性定义和脚本。自动刷新所有在线玩家属性。

### /na lookup [player] [page]

查看玩家的所有属性值。支持分页（每页 10 条）。

范围属性显示为 `min ~ max` 格式，百分比属性自动加 `%`。

### /na power [player]

查看玩家的战斗力数值。

### /na buff \<player\> \<id\> \<duration\> [attr] [value]

给玩家添加 Buff。

- `duration`：支持 tick 数字（`200`）或时间格式（`1d2h30m10s`），`-1` 为永久
- `attr` + `value`：可选，指定 Buff 的属性和值

```
/na buff Steve str_boost 5m physical_damage 50
/na buff Steve eternal -1 max_health 200
```

### /na unbuff \<player\> \<id\>

移除玩家的指定 Buff。

### /na panel [player]

打开属性面板 GUI。左键查看属性来源明细。

### /na debug [player]

显示调试信息：所有属性来源列表和 Buff 列表（含剩余时间）。

### /na refresh [player]

手动刷新玩家属性（强制重新计算）。

### /na mirror [clear]

打印性能分析报告，显示各模块的调用次数和耗时。

- `/na mirror` — 查看报告
- `/na mirror clear` — 清空数据
