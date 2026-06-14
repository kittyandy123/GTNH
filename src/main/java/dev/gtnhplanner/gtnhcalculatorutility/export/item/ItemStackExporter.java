package dev.gtnhplanner.gtnhcalculatorutility.export.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportStack;

public class ItemStackExporter {

    private static final Pattern MINECRAFT_FORMATTING_CODE = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");
    private final Set<String> displayNameFallbackItems = new HashSet<>();

    public ExportStack toExportStack(ItemStack stack) {
        return toExportStack(stack, getStackSize(stack));
    }

    public ExportStack toExportStack(ItemStack stack, int amount) {
        return new ExportStack(
            "item",
            getItemId(stack),
            getItemMeta(stack),
            getSafeDisplayName(stack),
            amount,
            "items");
    }

    private int getItemMeta(ItemStack stack) {
        if (stack == null) {
            return 0;
        }

        return stack.getItemDamage();
    }

    private int getStackSize(ItemStack stack) {
        if (stack == null) {
            return 0;
        }

        return stack.stackSize;
    }

    public String getItemId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "unknown";
        }

        Item item = stack.getItem();
        String itemId = Item.itemRegistry.getNameForObject(item);

        if (itemId == null || itemId.isEmpty()) {
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
                return cleanDisplayName(displayName);
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
        if (value == null || value.isEmpty()) {
            return "unknown";
        }

        String sanitized = value.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9._-]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");

        if (sanitized.isEmpty()) {
            return "unknown";
        }

        return sanitized;
    }

    private String cleanDisplayName(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return MINECRAFT_FORMATTING_CODE.matcher(value)
            .replaceAll("");
    }

}
