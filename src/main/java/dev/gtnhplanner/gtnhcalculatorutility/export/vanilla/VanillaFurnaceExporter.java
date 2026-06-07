package dev.gtnhplanner.gtnhcalculatorutility.export.vanilla;

import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import dev.gtnhplanner.gtnhcalculatorutility.export.item.ItemStackExporter;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.MachineInfo;

public class VanillaFurnaceExporter {

    private static final int FURNACE_DURATION_TICKS = 200;

    private final ItemStackExporter itemStackExporter;

    public VanillaFurnaceExporter(ItemStackExporter itemStackExporter) {
        this.itemStackExporter = itemStackExporter;
    }

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
            recipe.id = "minecraft:furnace:" + itemStackExporter.getStackId(input)
                + "_to_"
                + itemStackExporter.getStackId(output);
            recipe.machine = new MachineInfo("minecraft:furnace", "Furnace", "Minecraft");
            recipe.durationTicks = FURNACE_DURATION_TICKS;
            recipe.durationSeconds = FURNACE_DURATION_TICKS / 20.0;
            recipe.eut = 0;

            recipe.inputs.add(itemStackExporter.toExportStack(input));
            recipe.outputs.add(itemStackExporter.toExportStack(output));

            document.recipes.add(recipe);
        }
    }

}
