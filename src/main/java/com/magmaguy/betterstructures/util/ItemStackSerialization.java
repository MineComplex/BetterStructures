package com.magmaguy.betterstructures.util;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemStackSerialization {

    public static ItemStack serializeItem(Map<String, Object> deserializedItemStack) {
        try {
            return ItemStack.deserialize(deserializedItemStack);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
