package dev.gtnhplanner.gtnhcalculatorutility.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDiagnostics;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportStack;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.MachineInfo;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

public class RecipeExporter {

    private static final int FURNACE_DURATION_TICKS = 200;

    private int duplicateRecipesSkipped;

    public ExportResult exportRecipes(File minecraftDir) throws IOException {
        duplicateRecipesSkipped = 0;

        File exportDir = new File(minecraftDir, "gtnh-calculator-utility");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Failed to create export directory: " + exportDir.getAbsolutePath());
        }

        ExportDocument document = new ExportDocument();
        document.export.exportedAt = createUtcTimestamp();

        addTestDieselRecipe(document);
        addFurnaceRecipes(document);
        addGregTechRecipeMap(document, "gregtech:mixer", "Mixer", RecipeMaps.mixerRecipes);
        addGregTechRecipeMap(document, "gregtech:centrifuge", "Centrifuge", RecipeMaps.centrifugeRecipes);
        addGregTechRecipeMap(document, "gregtech:electrolyzer", "Electrolyzer", RecipeMaps.electrolyzerRecipes);
        addGregTechRecipeMap(
            document,
            "gregtech:chemical_reactor",
            "Chemical Reactor",
            RecipeMaps.chemicalReactorRecipes);

        deduplicateRecipes(document);
        populateDiagnostics(document);

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

    private void addGregTechRecipeMap(ExportDocument document, String machineId, String machineName,
        RecipeMap<?> recipeMap) {
        for (Object rawRecipe : recipeMap.getAllRecipes()) {
            if (!(rawRecipe instanceof GTRecipe)) {
                continue;
            }

            GTRecipe gtRecipe = (GTRecipe) rawRecipe;

            if (!gtRecipe.mEnabled || gtRecipe.mHidden || gtRecipe.mFakeRecipe) {
                continue;
            }

            ExportRecipe recipe = createGregTechRecipe(machineId, machineName, recipeMap, gtRecipe);
            document.recipes.add(recipe);
        }
    }

    private ExportRecipe createGregTechRecipe(String machineId, String machineName, RecipeMap<?> recipeMap,
        GTRecipe gtRecipe) {
        ExportRecipe recipe = new ExportRecipe();

        recipe.id = createRecipeId(machineId, gtRecipe);
        recipe.machine = new MachineInfo(machineId, machineName, "GregTech");
        recipe.durationTicks = gtRecipe.mDuration;
        recipe.durationSeconds = gtRecipe.mDuration / 20.0;
        recipe.eut = gtRecipe.mEUt;
        recipe.metadata.hidden = gtRecipe.mHidden;
        recipe.metadata.fakeRecipe = gtRecipe.mFakeRecipe;
        recipe.metadata.specialValue = gtRecipe.mSpecialValue;
        recipe.metadata.needsEmptyOutput = gtRecipe.mNeedsEmptyOutput;
        recipe.metadata.nbtSensitive = gtRecipe.isNBTSensitive;
        recipe.metadata.recipeMap = machineId;
        recipe.metadata.recipeMapUnlocalizedName = recipeMap.unlocalizedName;
        recipe.metadata.recipeCategory = String.valueOf(gtRecipe.getRecipeCategory());

        String neiDescription = firstNonEmpty(
            getOptionalStringValue(gtRecipe, "getNeiDesc"),
            getOptionalStringValue(gtRecipe, "getNEIDescription"),
            getOptionalStringValue(gtRecipe, "getNeiDescription"));
        if (neiDescription != null && !neiDescription.isEmpty()) {
            recipe.metadata.neiDescription = neiDescription;
        }

        addItemStacks(recipe, recipe.inputs, gtRecipe, gtRecipe.mInputs, true);
        addFluidStacks(recipe.inputs, gtRecipe, gtRecipe.mFluidInputs, true);
        addItemStacks(recipe, recipe.outputs, gtRecipe, gtRecipe.mOutputs, false);
        addFluidStacks(recipe.outputs, gtRecipe, gtRecipe.mFluidOutputs, false);

        return recipe;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        return null;
    }

    private String getOptionalStringValue(Object target, String methodName) {
        try {
            Method method = target.getClass()
                .getMethod(methodName);

            Object value = method.invoke(target);

            if (value instanceof String) {
                return (String) value;
            }
        } catch (Exception ignored) {

        }

        return null;
    }

    private void addItemStacks(ExportRecipe recipe, List<ExportStack> target, GTRecipe gtRecipe, ItemStack[] stacks,
        boolean input) {
        if (stacks == null) {
            return;
        }

        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];

            if (stack == null) {
                continue;
            }

            if (input && isProgrammedCircuit(stack)) {
                recipe.metadata.circuit = stack.getItemDamage();
                continue;
            }

            ExportStack exportStack = toExportStack(stack);

            int chance = input ? getChance(gtRecipe, "getInputChance", i) : getChance(gtRecipe, "getOutputChance", i);

            applyChance(exportStack, chance);
            target.add(exportStack);
        }
    }

    private void addFluidStacks(List<ExportStack> target, GTRecipe gtRecipe, FluidStack[] stacks, boolean input) {
        if (stacks == null) {
            return;
        }

        for (int i = 0; i < stacks.length; i++) {
            FluidStack stack = stacks[i];

            if (stack == null) {
                continue;
            }

            ExportStack exportStack = toExportStack(stack);

            int chance = input ? getChance(gtRecipe, "getFluidInputChance", i)
                : getChance(gtRecipe, "getFluidOutputChance", i);

            applyChance(exportStack, chance);
            target.add(exportStack);
        }
    }

    private void applyChance(ExportStack exportStack, int chance) {
        if (chance <= 0 || chance >= 10000) {
            return;
        }

        exportStack.chance = chance / 10000.0;
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

    private ExportStack toExportStack(FluidStack stack) {
        String fluidId = "unknown";
        String displayName = "Unknown Fluid";

        if (stack.getFluid() != null) {
            fluidId = stack.getFluid()
                .getName();
            displayName = stack.getLocalizedName();
        }

        return new ExportStack("fluid", fluidId, 0, displayName, stack.amount, "L");
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

    private boolean isProgrammedCircuit(ItemStack stack) {
        return "gregtech:gt.integrated_circuit".equals(getItemId(stack));
    }

    private String createRecipeId(String machineId, GTRecipe recipe) {
        StringBuilder identity = new StringBuilder();

        identity.append(machineId)
            .append('|')
            .append(recipe.mDuration)
            .append('|')
            .append(recipe.mEUt)
            .append('|')
            .append(recipe.mSpecialValue)
            .append('|')
            .append(recipe.mNeedsEmptyOutput)
            .append('|')
            .append(recipe.isNBTSensitive)
            .append('|')
            .append(String.valueOf(recipe.getRecipeCategory()))
            .append('|')
            .append(String.valueOf(recipe.mSpecialItems))
            .append('|');

        appendItemChances(identity, recipe, recipe.mInputs, true);
        identity.append('|');
        appendItemChances(identity, recipe, recipe.mOutputs, false);
        identity.append('|');
        appendFluidChances(identity, recipe, recipe.mFluidInputs, true);
        identity.append('|');
        appendFluidChances(identity, recipe, recipe.mFluidOutputs, false);
        identity.append('|');

        appendItemStacks(identity, recipe.mInputs);
        identity.append('|');
        appendFluidStacks(identity, recipe.mFluidInputs);
        identity.append('|');
        appendItemStacks(identity, recipe.mOutputs);
        identity.append('|');
        appendFluidStacks(identity, recipe.mFluidOutputs);

        return machineId + ":" + sha1Short(identity.toString());
    }

    private void appendItemChances(StringBuilder identity, GTRecipe recipe, ItemStack[] stacks, boolean input) {
        if (stacks == null) {
            return;
        }

        String methodName = input ? "getInputChance" : "getOutputChance";

        for (int i = 0; i < stacks.length; i++) {
            identity.append(getChance(recipe, methodName, i))
                .append(';');
        }
    }

    private void appendFluidChances(StringBuilder identity, GTRecipe recipe, FluidStack[] stacks, boolean input) {
        if (stacks == null) {
            return;
        }

        String methodName = input ? "getFluidInputChance" : "getFluidOutputChance";

        for (int i = 0; i < stacks.length; i++) {
            identity.append(getChance(recipe, methodName, i))
                .append(';');
        }
    }

    private int getChance(GTRecipe recipe, String methodName, int index) {
        try {
            Method method = recipe.getClass()
                .getMethod(methodName, int.class);

            Object value = method.invoke(recipe, index);

            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (Exception ignored) {

        }

        return 10000;
    }

    private void appendItemStacks(StringBuilder identity, ItemStack[] stacks) {
        if (stacks == null) {
            return;
        }

        for (ItemStack stack : stacks) {
            if (stack == null) {
                continue;
            }

            identity.append("item:")
                .append(getItemId(stack))
                .append(':')
                .append(stack.getItemDamage())
                .append(':')
                .append(stack.stackSize)
                .append(';');
        }
    }

    private void appendFluidStacks(StringBuilder identity, FluidStack[] stacks) {
        if (stacks == null) {
            return;
        }

        for (FluidStack stack : stacks) {
            if (stack == null) {
                continue;
            }

            String fluidId = "unknown";
            if (stack.getFluid() != null) {
                fluidId = stack.getFluid()
                    .getName();
            }

            identity.append("fluid:")
                .append(fluidId)
                .append(':')
                .append(stack.amount)
                .append(';');
        }
    }

    private String sha1Short(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 8 && i < hash.length; i++) {
                result.append(String.format("%02x", hash[i] & 0xff));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm was not available", e);
        }
    }

    private ExportResult createResult(File outputFile, ExportDocument document) {
        if (duplicateRecipesSkipped > 0) {
            System.out.println("GTNH Calculator Utility skipped duplicate recipes: " + duplicateRecipesSkipped);
        }

        return new ExportResult(
            outputFile,
            document.recipes.size(),
            duplicateRecipesSkipped,
            countRecipesByMachine(document));
    }

    private void deduplicateRecipes(ExportDocument document) {
        Set<String> seenRecipeIds = new HashSet<>();
        List<ExportRecipe> deduplicatedRecipes = new ArrayList<>();

        for (ExportRecipe recipe : document.recipes) {
            if (recipe.id == null) {
                deduplicatedRecipes.add(recipe);
                continue;
            }

            if (seenRecipeIds.add(recipe.id)) {
                deduplicatedRecipes.add(recipe);
            } else {
                duplicateRecipesSkipped++;
            }
        }

        document.recipes = deduplicatedRecipes;
    }

    private void populateDiagnostics(ExportDocument document) {
        ExportDiagnostics diagnostics = new ExportDiagnostics();

        diagnostics.totalRecipes = document.recipes.size();
        diagnostics.duplicateRecipesSkipped = duplicateRecipesSkipped;
        diagnostics.recipeCountsByMachine.putAll(countRecipesByMachine(document));

        document.diagnostics = diagnostics;
    }

    private Map<String, Integer> countRecipesByMachine(ExportDocument document) {
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

        return recipeCountByMachine;
    }

    private String createUtcTimestamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(new Date());
    }

    private void writeJson(File outputFile, ExportDocument document) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting()
            .create();

        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(document, writer);
        }
    }

}
