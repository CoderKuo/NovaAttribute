package github.saukiya.sxattribute.data.eventdata.sub;

import github.saukiya.sxattribute.data.eventdata.EventData;
import org.bukkit.entity.LivingEntity;

public class UpdateData implements EventData {

    private final LivingEntity entity;

    public UpdateData(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }
}
