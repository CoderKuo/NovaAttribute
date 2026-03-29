package github.saukiya.sxattribute.event;

import github.saukiya.sxattribute.data.attribute.SXAttributeData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SXGetAttributeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final LivingEntity entity;
    private SXAttributeData data;

    public SXGetAttributeEvent(LivingEntity entity, SXAttributeData data, boolean isAsync) {
        super(isAsync);
        this.entity = entity;
        this.data = data;
    }

    public LivingEntity getEntity() { return entity; }
    public SXAttributeData getData() { return data; }
    public void setData(SXAttributeData data) { this.data = data; }

    public static HandlerList getHandlerList() { return handlers; }
    public HandlerList getHandlers() { return handlers; }
}
