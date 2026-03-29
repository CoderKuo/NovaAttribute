package github.saukiya.sxattribute.data.attribute;

import github.saukiya.sxattribute.data.eventdata.EventData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SX2 兼容 - 属性抽象基类
 * SX2 附属继承此类并调用 registerAttribute() 注册自定义属性
 * 桥接到 NovaAttribute 的属性系统
 */
public abstract class SubAttribute implements Comparable<SubAttribute> {

    private static final List<SubAttribute> attributes = Collections.synchronizedList(new ArrayList<>());

    private final String name;
    private final JavaPlugin plugin;
    private final AttributeType[] types;
    private int length;
    private int priority = -1;

    public SubAttribute(JavaPlugin plugin, int length, AttributeType... types) {
        this(null, plugin, length, types);
    }

    public SubAttribute(String name, JavaPlugin plugin, int length, AttributeType... types) {
        this.name = name == null ? getClass().getSimpleName() : name;
        this.plugin = plugin;
        this.length = length;
        this.types = types.length == 0 ? new AttributeType[]{AttributeType.OTHER} : types;
        // 自动分配优先级：基于注册顺序，起始值 1000 避免和 NovaAttribute 内置属性冲突
        this.priority = 1000 + attributes.size();
    }

    /**
     * 注册属性到 NovaAttribute 兼容层
     */
    public void registerAttribute() {
        if (plugin == null) {
            System.out.println("[NovaAttribute-SX2] Attribute >> [NULL|" + name + "] Null Plugin!");
            return;
        }
        // 检查重复
        for (int i = 0; i < attributes.size(); i++) {
            if (attributes.get(i).getName().equals(this.name)) {
                attributes.set(i, this);
                System.out.println("[NovaAttribute-SX2] Attribute >> [" + plugin.getName() + "|" + name + "] replaced existing.");
                // 通知桥接管理器更新
                com.dakuo.novaattribute.compat.sx2.SX2Bridge.INSTANCE.onAttributeRegistered(this);
                return;
            }
        }
        attributes.add(this);
        Collections.sort(attributes);
        System.out.println("[NovaAttribute-SX2] Attribute >> Registered [" + plugin.getName() + "|" + name + "] priority=" + priority);
        // 通知桥接管理器
        com.dakuo.novaattribute.compat.sx2.SX2Bridge.INSTANCE.onAttributeRegistered(this);
    }

    // ====== 抽象方法（SX2 附属必须实现）======

    public abstract void eventMethod(double[] values, EventData eventData);
    public abstract Object getPlaceholder(double[] values, Player player, String string);
    public abstract List<String> getPlaceholders();
    public abstract void loadAttribute(double[] values, String lore);

    // ====== 生命周期 ======

    public void onEnable() {}
    public void onReLoad() {}
    public void onDisable() {}

    // ====== 数据方法 ======

    public void correct(double[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.max(values[i], 0D);
        }
    }

    public double calculationCombatPower(double[] values) {
        return 0D;
    }

    public final boolean containsType(AttributeType attributeType) {
        return Arrays.asList(types).contains(attributeType);
    }

    // ====== Getter ======

    public String getName() { return name; }
    public JavaPlugin getPlugin() { return plugin; }
    public AttributeType[] getTypes() { return types; }
    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    public int getPriority() { return priority; }

    // ====== 静态工具 ======

    public static List<SubAttribute> getAttributes() { return attributes; }

    public static SubAttribute getSubAttribute(String attributeName) {
        for (SubAttribute attr : attributes) {
            if (attr.getName().equals(attributeName)) return attr;
        }
        return null;
    }

    public static boolean probability(double value) {
        return value > 0 && value / 100D > Math.random();
    }

    public static double getNumber(String lore) {
        String cleaned = lore.replaceAll("§[a-z0-9]", "").replaceAll("<#[a-fA-F0-9]{6}>", "");
        Matcher matcher = Pattern.compile("[-+]?\\d+(\\.\\d+)?").matcher(cleaned);
        if (matcher.find()) {
            try { return Double.parseDouble(matcher.group()); }
            catch (NumberFormatException e) { return 0D; }
        }
        return 0D;
    }

    @Override
    public int compareTo(SubAttribute other) {
        return Integer.compare(this.priority, other.priority);
    }
}
