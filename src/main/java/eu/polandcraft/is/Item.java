package eu.polandcraft.is;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class Item {
    private int id;
    private int subId;
    private int amount;


    public Item(int id, int subId, int amount) {
        this.id = id;
        this.subId = subId;
        this.amount = amount;
    }

    public String getNiceName() {
        StringBuilder bob = new StringBuilder();
        Material m = Material.getMaterial(id);
        for (String string : m.name().toLowerCase().split("_")) {
            bob.append(string.substring(0, 1).toUpperCase());
            bob.append(string.substring(1).toLowerCase());
            bob.append(" ");
        }
        return bob.substring(0, bob.length() - 1);
    }

    public ItemStack getItemStack() {
        try {
            return new ItemStack(id, amount, (short) subId);
        } catch (Exception e) {
            return new ItemStack(id, amount);
        }
    }
}
