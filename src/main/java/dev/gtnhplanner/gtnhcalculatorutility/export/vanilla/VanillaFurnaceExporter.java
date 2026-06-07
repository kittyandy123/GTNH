package dev.gtnhplanner.gtnhcalculatorutility.export.vanilla;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportStack;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.MachineInfo;

public class VanillaFurnaceExporter {

    private static final int FURNACE_DURATION_TICKS = 200;

    public void addRecipes(ExportDocument document) {
        Map<?, ?> furnaceRecipes = FurnaceRecipes.smelting()
            .getSmeltingList();

        for (Map.Entry<?, ?> entry : furnaceRecipes.entrySet()) {
            if (!(entry.getKey() instanceof ItemStack) || !(entry.getValue() instanceof ItemStack)) {
                continue;
            }

            ItemStack input = (ItemStack) entry.getKey();
            ItemStack output = (ItemStack) entry.getValue();

            ExportRecipe recipe = new ExportRecipe();
            recipe.id = "minecraft:furnace:" + getStackId(input) + "_to_" + getStackId(output);
            recipe.machine = new MachineInfo("minecraft:furnace", "Furnace", "Minecraft");
            recipe.durationTicks = FURNACE_DURATION_TICKS;
            recipe.durationSeconds = FURNACE_DURATION_TICKS / 20.0;
            recipe.eut = 0;

            recipe.inputs.add(toExportStack(input));
            recipe.outputs.add(toExportStack(output));

            document.recipes.add(recipe);
        }
    }

    private ExportStack toExportStack(ItemStack stack) {
        return new ExportStack(
            "item",
            getItemId(stack),
            stack.getItemDamage(),
            stack.getDisplayName(),
            stack.stackSize,
            "items");
    }

    private String getItemId(ItemStack stack) {
        Item item = stack.getItem();
        String itemId = Item.itemRegistry.getNameForObject(item);

        if (itemId == null) {
            return "unknown";
        }

        return itemId;
    }

    private String getStackId(ItemStack stack) {
        return sanitizeId(getItemId(stack) + "_" + stack.getItemDamage());
    }

    private String sanitizeId(String value) {
        return value.toLowerCase()
            .replace(":", "_")
            .replace(" ", "_")
            .replace("/", "_");
    }

}
