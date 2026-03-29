package github.saukiya.sxattribute.event;

import github.saukiya.sxattribute.data.eventdata.sub.DamageData;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SXDamageEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final DamageData data;

    public SXDamageEvent(DamageData data) {
        this.data = data;
    }

    public DamageData getData() { return data; }

    public static HandlerList getHandlerList() { return handlers; }
    public HandlerList getHandlers() { return handlers; }
}
