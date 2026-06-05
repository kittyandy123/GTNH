package dev.gtnhplanner.gtnhcalculatorutility.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportStack;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.MachineInfo;

public class RecipeExporter {

    private static final int FURNACE_DURATION_TICKS = 200;

    public ExportResult exportRecipes(File minecraftDir) throws IOException {
        File exportDir = new File(minecraftDir, "gtnh-calculator-utility");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Failed to create export directory: " + exportDir.getAbsolutePath());
        }

        ExportDocument document = new ExportDocument();

        addTestDieselRecipe(document);
        addFurnaceRecipes(document);

        File outputFile = new File(exportDir, "recipes-test.json");
        writeJson(outputFile, document);

        return createResult(outputFile, document);
    }

    private void addTestDieselRecipe(ExportDocument document) {

        ExportRecipe recipe = new ExportRecipe();

        recipe.id = "test:mixer:light_fuel_heavy_fuel_to_diesel";
        recipe.machine = new MachineInfo("gregtech:mixer", "Mixer", "GregTech");
        recipe.durationTicks = 16;
        recipe.durationSeconds = 0.8;
        recipe.eut = 30;

        recipe.inputs.add(new ExportStack("fluid", "light_fuel", 0, "Light Fuel", 5000, "L"));
        recipe.inputs.add(new ExportStack("fluid", "heavy_fuel", 0, "Heavy Fuel", 1000, "L"));
        recipe.outputs.add(new ExportStack("fluid", "diesel", 0, "Diesel", 6000, "L"));

        document.recipes.add(recipe);
    }

    private void addFurnaceRecipes(ExportDocument document) {

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

    private ExportResult createResult(File outputFile, ExportDocument document) {

        Map<String, Integer> recipeCountByMachine = new LinkedHashMap<>();

        for (ExportRecipe recipe : document.recipes) {
            String machineId = "unknown";

            if (recipe.machine != null && recipe.machine.id != null) {
                machineId = recipe.machine.id;
            }

            Integer currentCount = recipeCountByMachine.get(machineId);
            if (currentCount == null) {
                recipeCountByMachine.put(machineId, 1);
            } else {
                recipeCountByMachine.put(machineId, currentCount + 1);
            }
        }

        return new ExportResult(outputFile, document.recipes.size(), recipeCountByMachine);
    }

    private void writeJson(File outputFile, ExportDocument document) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting()
            .create();

        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(document, writer);
        }
    }

}
