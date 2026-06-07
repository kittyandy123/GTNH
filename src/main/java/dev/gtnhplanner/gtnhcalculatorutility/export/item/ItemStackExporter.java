package dev.gtnhplanner.gtnhcalculatorutility.export.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportStack;

public class ItemStackExporter {

    private final Set<String> displayNameFallbackItems = new HashSet<>();

    public ExportStack toExportStack(ItemStack stack) {
        return new ExportStack(
            "item",
            getItemId(stack),
            stack.getItemDamage(),
            getSafeDisplayName(stack),
            stack.stackSize,
            "items");
    }

    public String getItemId(ItemStack stack) {
        Item item = stack.getItem();
        String itemId = Item.itemRegistry.getNameForObject(item);

        if (itemId == null) {
            return "unknown";
        }

        return itemId;
    }

    public String getStackId(ItemStack stack) {
        return sanitizeId(getItemId(stack) + "_" + stack.getItemDamage());
    }

    public List<String> getDisplayNameFallbackItems() {
        List<String> fallbackItems = new ArrayList<>(displayNameFallbackItems);
        Collections.sort(fallbackItems);
        return fallbackItems;
    }

    private String getSafeDisplayName(ItemStack stack) {
        try {
            String displayName = stack.getDisplayName();

            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            }
        } catch (Exception e) {
            String warningKey = getItemId(stack) + ":" + stack.getItemDamage();

            if (displayNameFallbackItems.add(warningKey)) {
                System.out.println(
                    "GTNH Calculator Utility could not read display name for item " + warningKey
                        + " - "
                        + e.getClass()
                            .getSimpleName());
            }
        }

        return getItemId(stack) + ":" + stack.getItemDamage();
    }

    private String sanitizeId(String value) {
        return value.toLowerCase()
            .replace(":", "_")
            .replace(" ", "_")
            .replace("/", "_");
    }

}
