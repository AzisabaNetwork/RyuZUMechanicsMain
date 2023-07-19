package me.deecaad.weaponmechanics.weapon.skin;

import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;

public class BaseSkin implements Skin, Serializer<BaseSkin>  {

    private Material type;
    private byte data;
    private short durability;
    private int customModelData;

    /**
     * Default constructor for Serializer
     */
    public BaseSkin() {
    }

    public BaseSkin(Material type, byte data, short durability, int customModelData) {
        this.type = type;
        this.data = data;
        this.durability = durability;
        this.customModelData = customModelData;
    }

    public Material getType() {
        return type;
    }

    public byte getData() {
        return data;
    }

    public short getDurability() {
        return durability;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    /**
     * Applies this skin to given item stack
     *
     * @param itemStack the item stack for which to apply skin
     */
    public void apply(ItemStack itemStack) {
        double version = CompatibilityAPI.getVersion();

        if (type != null && itemStack.getType() != type) {
            itemStack.setType(type);
        }

        if (data != -1 && version < 1.13 && itemStack.getData().getData() != data) {
            itemStack.getData().setData(data);
        }

        boolean hasMetaChanges = false;
        ItemMeta meta = itemStack.getItemMeta();

        // Happens when somebody tries to apply a skin to air
        if (meta == null)
            throw new IllegalArgumentException("Tried to apply skin to item without meta: " + itemStack);

        if (durability != -1) {
            if (version >= 1.132) {
                if (((org.bukkit.inventory.meta.Damageable) meta).getDamage() != durability) {
                    ((org.bukkit.inventory.meta.Damageable) meta).setDamage(durability);
                    hasMetaChanges = true;
                }
            } else if (itemStack.getDurability() != durability) {
                itemStack.setDurability(durability);
                hasMetaChanges = true;
            }
        }

        if (customModelData != -1 && version >= 1.14
                && (!meta.hasCustomModelData() || meta.getCustomModelData() != customModelData)) {
            meta.setCustomModelData(customModelData);
            hasMetaChanges = true;
        }

        if (hasMetaChanges) {
            itemStack.setItemMeta(meta);
        }
    }

    @Override
    @Nonnull
    public BaseSkin serialize(SerializeData data) throws SerializerException {

        Material type = data.of("Type").getEnum(Material.class, null);
        byte legacyData = (byte) data.of("Legacy_Data").assertPositive().getInt(-1);
        short durability = (short) data.of("Durability").assertPositive().getInt(-1);
        int customModelData = data.of("Custom_Model_Data").assertPositive().getInt(-1);

        return new BaseSkin(type, legacyData, durability, customModelData);
    }
}