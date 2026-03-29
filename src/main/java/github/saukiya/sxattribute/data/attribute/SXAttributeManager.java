package github.saukiya.sxattribute.data.attribute;

import com.dakuo.novaattribute.core.attribute.AttributeData;
import com.dakuo.novaattribute.core.attribute.AttributeManager;
import com.dakuo.novaattribute.core.attribute.AttributeMap;
import com.dakuo.novaattribute.core.reader.ItemAttributeReader;
import com.dakuo.novaattribute.core.reader.LoreReader;
import github.saukiya.sxattribute.data.PreLoadItem;
import org.bukkit.entity.LivingEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SXAttributeManager {

    private final Map<UUID, SXAttributeData> entityDataMap = new ConcurrentHashMap<>();

    public Map<UUID, SXAttributeData> getEntityDataMap() {
        return entityDataMap;
    }

    public SXAttributeData getEntityData(LivingEntity entity) {
        AttributeMap novaMap = AttributeManager.INSTANCE.getOrNull(entity);
        if (novaMap == null) return new SXAttributeData();
        Map<String, List<Double>> doubleListMap = novaToDoubleListMap(novaMap);
        return SXAttributeData.fromNovaData(AttributeData.Companion.fromMap(doubleListMap));
    }

    public void loadEntityData(LivingEntity entity) {
        AttributeManager.INSTANCE.refresh(entity, com.dakuo.novaattribute.api.event.RefreshCause.API_CALL);
    }

    public void loadEntityData(LivingEntity entity, boolean isAsync) {
        loadEntityData(entity);
    }

    public SXAttributeData loadListData(List<String> list) {
        AttributeData novaData = LoreReader.INSTANCE.readLines(list);
        if (novaData == null) return new SXAttributeData();
        return SXAttributeData.fromNovaData(novaData);
    }

    public SXAttributeData loadItemData(LivingEntity entity, List<PreLoadItem> items) {
        SXAttributeData result = new SXAttributeData();
        for (PreLoadItem item : items) {
            if (item.getItem() == null) continue;
            AttributeData novaData = ItemAttributeReader.INSTANCE.read(item.getItem());
            if (!novaData.isEmpty()) {
                result.add(SXAttributeData.fromNovaData(novaData));
            }
        }
        return result;
    }

    public void clearEntityData(UUID uuid) {
        entityDataMap.remove(uuid);
    }

    public void attributeUpdateEvent(LivingEntity entity) {
        com.dakuo.novaattribute.compat.sx2.SX2Bridge.INSTANCE.executeUpdateAttributes(entity);
    }

    private Map<String, List<Double>> novaToDoubleListMap(AttributeMap novaMap) {
        Map<String, Double> all = novaMap.getAll();
        Map<String, List<Double>> map = new HashMap<>();
        for (Map.Entry<String, Double> entry : all.entrySet()) {
            map.put(entry.getKey(), Collections.singletonList(entry.getValue()));
        }
        return map;
    }
}
