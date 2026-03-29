package github.saukiya.sxattribute.event;

import github.saukiya.sxattribute.data.PreLoadItem;
import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class SXLoadAttributeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final LivingEntity entity;
    private final List<PreLoadItem> itemList;
    private final SXAttributeData attributeData;

    public SXLoadAttributeEvent(LivingEntity entity, List<PreLoadItem> itemList, SXAttributeData attributeData) {
        this.entity = entity;
        this.itemList = itemList;
        this.attributeData = attributeData;
    }

    public SXLoadAttributeEvent(LivingEntity entity, List<PreLoadItem> itemList, SXAttributeData attributeData, boolean isAsync) {
        super(isAsync);
        this.entity = entity;
        this.itemList = itemList;
        this.attributeData = attributeData;
    }

    public LivingEntity getEntity() { return entity; }
    public List<PreLoadItem> getItemList() { return itemList; }
    public SXAttributeData getAttributeData() { return attributeData; }

    public static HandlerList getHandlerList() { return handlers; }
    public HandlerList getHandlers() { return handlers; }
}
