package github.saukiya.sxattribute.data.eventdata.sub;

import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import github.saukiya.sxattribute.data.eventdata.EventData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * SX2 兼容 - 伤害数据
 * 桥接到 NovaAttribute 的 DamageContext
 */
public class DamageData implements EventData {

    private final LivingEntity defender;
    private final LivingEntity attacker;
    private final String defenderName;
    private final String attackerName;
    private final SXAttributeData defenderData;
    private final SXAttributeData attackerData;
    private final EntityDamageByEntityEvent event;
    private final List<String> effectiveAttributeList = new ArrayList<>();
    private final List<String> holoList = new ArrayList<>();
    private double damage;
    private boolean crit;
    private boolean cancelled = false;

    // 持有 NovaAttribute 的 DamageContext 引用，用于双向同步
    private final Object novaContext;

    public DamageData(LivingEntity defender, LivingEntity attacker,
                      SXAttributeData defenderData, SXAttributeData attackerData,
                      EntityDamageByEntityEvent event, Object novaContext) {
        this.defender = defender;
        this.attacker = attacker;
        this.defenderName = defender.getName();
        this.attackerName = attacker.getName();
        this.defenderData = defenderData;
        this.attackerData = attackerData;
        this.event = event;
        this.damage = event.getDamage();
        this.novaContext = novaContext;
    }

    public LivingEntity getDefender() { return defender; }
    public LivingEntity getAttacker() { return attacker; }
    public String getDefenderName() { return defenderName; }
    public String getAttackerName() { return attackerName; }
    public SXAttributeData getDefenderData() { return defenderData; }
    public SXAttributeData getAttackerData() { return attackerData; }
    public EntityDamageByEntityEvent getEvent() { return event; }
    public List<String> getEffectiveAttributeList() { return effectiveAttributeList; }
    public List<String> getHoloList() { return holoList; }
    public double getDamage() { return damage; }
    public boolean isCrit() { return crit; }
    public boolean isCancelled() { return cancelled; }
    public Object getNovaContext() { return novaContext; }

    public void setCrit(boolean crit) { this.crit = crit; }

    public void sendHolo(String message) {
        if (message != null && !message.contains("Null Message: ")) {
            holoList.add(message);
        }
    }

    public void setDamage(double damage) {
        this.damage = damage;
        syncToNova();
    }

    public void addDamage(double addDamage) {
        this.damage += addDamage;
        syncToNova();
    }

    public void takeDamage(double takeDamage) {
        this.damage -= takeDamage;
        syncToNova();
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        event.setCancelled(cancelled);
    }

    /**
     * 将 SX2 DamageData 的伤害值同步回 NovaAttribute 的 DamageContext
     */
    private void syncToNova() {
        if (novaContext instanceof com.dakuo.novaattribute.combat.DamageContext) {
            ((com.dakuo.novaattribute.combat.DamageContext) novaContext).setDamage(damage);
        }
    }
}
