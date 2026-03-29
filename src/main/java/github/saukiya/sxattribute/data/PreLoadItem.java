package github.saukiya.sxattribute.data;

import github.saukiya.sxattribute.data.condition.EquipmentType;
import org.bukkit.inventory.ItemStack;

public class PreLoadItem {

    private final EquipmentType type;
    private final ItemStack item;

    public PreLoadItem(EquipmentType type, ItemStack item) {
        this.type = type;
        this.item = item;
    }

    public PreLoadItem(ItemStack item) {
        this(EquipmentType.ALL, item);
    }

    public EquipmentType getType() { return type; }
    public ItemStack getItem() { return item; }
}
