package github.saukiya.sxattribute.api;

import com.dakuo.novaattribute.api.NovaAttributeAPI;
import com.dakuo.novaattribute.core.attribute.AttributeManager;
import github.saukiya.sxattribute.SXAttribute;
import github.saukiya.sxattribute.data.PreLoadItem;
import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import github.saukiya.sxattribute.data.condition.EquipmentType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SXAPI {

    private static final Map<UUID, Map<Class<?>, SXAttributeData>> map = new ConcurrentHashMap<>();

    public SXAttributeData getAPIAttribute(UUID uuid) {
        SXAttributeData result = new SXAttributeData();
        for (SXAttributeData data : map.getOrDefault(uuid, new HashMap<>()).values()) {
            result.add(data);
        }
        return result;
    }

    public void setProjectileData(UUID uuid, SXAttributeData attributeData) {
        if (attributeData != null && attributeData.isValid()) {
            SXAttribute.getAttributeManager().getEntityDataMap().put(uuid, attributeData);
        }
    }

    public SXAttributeData getProjectileData(UUID uuid) {
        return SXAttribute.getAttributeManager().getEntityDataMap().get(uuid);
    }

    public SXAttributeData getEntityData(LivingEntity entity) {
        return SXAttribute.getAttributeManager().getEntityData(entity);
    }

    public SXAttributeData getEntityAPIData(Class<?> c, UUID uuid) {
        return map.containsKey(uuid) ? map.get(uuid).get(c) : null;
    }

    public boolean hasEntityAPIData(Class<?> c, UUID uuid) {
        return map.containsKey(uuid) && map.get(uuid).containsKey(c);
    }

    public void setEntityAPIData(Class<?> c, UUID uuid, SXAttributeData attributeData) {
        map.computeIfAbsent(uuid, k -> new HashMap<>()).put(c, attributeData);
        if (attributeData != null && attributeData.isValid()) {
            Entity entity = org.bukkit.Bukkit.getServer().getEntity(uuid);
            if (entity instanceof LivingEntity) {
                NovaAttributeAPI.INSTANCE.updateSource(
                    (LivingEntity) entity,
                    "sx2api:" + c.getSimpleName(),
                    attributeData.toNovaData()
                );
            }
        }
    }

    public SXAttributeData removeEntityAPIData(Class<?> c, UUID uuid) {
        Map<Class<?>, SXAttributeData> m = map.get(uuid);
        SXAttributeData removed = m != null ? m.remove(c) : null;
        Entity entity = org.bukkit.Bukkit.getServer().getEntity(uuid);
        if (entity instanceof LivingEntity) {
            NovaAttributeAPI.INSTANCE.removeSource((LivingEntity) entity, "sx2api:" + c.getSimpleName());
        }
        return removed;
    }

    public void removePluginAllEntityData(Class<?> c) {
        for (Map.Entry<UUID, Map<Class<?>, SXAttributeData>> entry : map.entrySet()) {
            if (entry.getValue().remove(c) != null) {
                Entity entity = org.bukkit.Bukkit.getServer().getEntity(entry.getKey());
                if (entity instanceof LivingEntity) {
                    NovaAttributeAPI.INSTANCE.removeSource((LivingEntity) entity, "sx2api:" + c.getSimpleName());
                }
            }
        }
    }

    public void removeEntityAllPluginData(UUID uuid) {
        Map<Class<?>, SXAttributeData> removed = map.remove(uuid);
        if (removed != null) {
            Entity entity = org.bukkit.Bukkit.getServer().getEntity(uuid);
            if (entity instanceof LivingEntity) {
                for (Class<?> c : removed.keySet()) {
                    NovaAttributeAPI.INSTANCE.removeSource((LivingEntity) entity, "sx2api:" + c.getSimpleName());
                }
            }
        }
    }

    public boolean isUse(LivingEntity entity, EquipmentType type, List<String> list) {
        return true;
    }

    public SXAttributeData loadListData(List<String> list) {
        return SXAttribute.getAttributeManager().loadListData(list);
    }

    public SXAttributeData loadItemData(LivingEntity entity, PreLoadItem... preLoadItems) {
        return SXAttribute.getAttributeManager().loadItemData(entity, Arrays.asList(preLoadItems));
    }

    public String getEntityName(LivingEntity entity) {
        return entity.getName();
    }

    public int getItemLevel(ItemStack item) {
        return -1;
    }

    public void updateData(LivingEntity entity) {
        AttributeManager.INSTANCE.refresh(entity, com.dakuo.novaattribute.api.event.RefreshCause.API_CALL);
    }

    public void attributeUpdate(LivingEntity entity) {
        SXAttribute.getAttributeManager().attributeUpdateEvent(entity);
    }

    public ItemStack getItem(String itemKey, Player player) {
        return null;
    }

    public double getMaxHealth(LivingEntity entity) {
        return entity.getMaxHealth();
    }

    public boolean hasItem(String itemKey) {
        return false;
    }

    public Set<String> getItemList() {
        return Collections.emptySet();
    }
}
