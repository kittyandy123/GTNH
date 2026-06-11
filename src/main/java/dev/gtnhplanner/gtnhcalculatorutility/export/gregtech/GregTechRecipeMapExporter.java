package dev.gtnhplanner.gtnhcalculatorutility.export.gregtech;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import dev.gtnhplanner.gtnhcalculatorutility.export.item.ItemStackExporter;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportStack;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.MachineInfo;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

public class GregTechRecipeMapExporter {

    private final ItemStackExporter itemStackExporter;
    private int recipesSkippedDueToError;
    private final Map<String, Integer> recipeErrorsByMachine = new LinkedHashMap<>();
    private int toolInputsExtracted;
    private final Map<String, Integer> toolInputsByMachine = new LinkedHashMap<>();
    private int zeroAmountInputsMovedToTools;
    private int zeroAmountInputsRemaining;
    private int inferredToolAmounts;
    private final List<String> sampleToolInputs = new ArrayList<>();

    public GregTechRecipeMapExporter(ItemStackExporter itemStackExporter) {
        this.itemStackExporter = itemStackExporter;
    }

    public void addRecipeMap(ExportDocument document, String machineId, String machineName, RecipeMap<?> recipeMap) {
        for (Object rawRecipe : recipeMap.getAllRecipes()) {
            if (!(rawRecipe instanceof GTRecipe)) {
                continue;
            }

            GTRecipe gtRecipe = (GTRecipe) rawRecipe;

            if (!gtRecipe.mEnabled || gtRecipe.mHidden || gtRecipe.mFakeRecipe) {
                continue;
            }

            try {
                ExportRecipe recipe = createGregTechRecipe(machineId, machineName, recipeMap, gtRecipe);
                document.recipes.add(recipe);
            } catch (Exception e) {
                recordRecipeError(machineId);

                System.out.println(
                    "GTNH Calculator Utility skipped recipe in map " + machineId
                        + " due to "
                        + e.getClass()
                            .getSimpleName()
                        + ": "
                        + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public int getRecipesSkippedDueToError() {
        return recipesSkippedDueToError;
    }

    public Map<String, Integer> getRecipeErrorsByMachine() {
        return new LinkedHashMap<>(recipeErrorsByMachine);
    }

    public int getToolInputsExtracted() {
        return toolInputsExtracted;
    }

    public Map<String, Integer> getToolInputsByMachine() {
        return new LinkedHashMap<>(toolInputsByMachine);
    }

    public int getZeroAmountInputsMovedToTools() {
        return zeroAmountInputsMovedToTools;
    }

    public int getZeroAmountInputsRemaining() {
        return zeroAmountInputsRemaining;
    }

    public int getInferredToolAmounts() {
        return inferredToolAmounts;
    }

    public List<String> getSampleToolInputs() {
        return new ArrayList<>(sampleToolInputs);
    }

    private void recordRecipeError(String machineId) {
        recipesSkippedDueToError++;

        Integer currentCount = recipeErrorsByMachine.get(machineId);
        if (currentCount == null) {
            recipeErrorsByMachine.put(machineId, 1);
        } else {
            recipeErrorsByMachine.put(machineId, currentCount + 1);
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

        addItemStacks(recipe, recipe.inputs, gtRecipe, gtRecipe.mInputs, true, machineId);
        addFluidStacks(recipe.inputs, gtRecipe, gtRecipe.mFluidInputs, true);
        addItemStacks(recipe, recipe.outputs, gtRecipe, gtRecipe.mOutputs, false, machineId);
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
        boolean input, String machineId) {
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

            if (input && isNonConsumedToolCandidate(stack)) {
                ExportStack toolStack = itemStackExporter.toExportStack(stack, getToolAmount(stack));
                addTool(recipe, toolStack);
                recordToolInput(machineId, stack, toolStack);
                continue;
            }

            ExportStack exportStack = itemStackExporter.toExportStack(stack);

            if (input && exportStack.amount <= 0) {
                zeroAmountInputsRemaining++;
            }

            int chance = input ? getChance(gtRecipe, "getInputChance", i) : getChance(gtRecipe, "getOutputChance", i);

            applyChance(exportStack, chance);
            target.add(exportStack);
        }
    }

    private boolean isNonConsumedToolCandidate(ItemStack stack) {
        return stack.stackSize <= 0;
    }

    private int getToolAmount(ItemStack stack) {
        if (stack.stackSize <= 0) {
            inferredToolAmounts++;
            return 1;
        }

        return stack.stackSize;
    }

    private void addTool(ExportRecipe recipe, ExportStack toolStack) {
        if (recipe.tools == null) {
            recipe.tools = new ArrayList<>();
        }

        recipe.tools.add(toolStack);
    }

    private void recordToolInput(String machineId, ItemStack sourceStack, ExportStack toolStack) {
        toolInputsExtracted++;

        if (sourceStack.stackSize <= 0) {
            zeroAmountInputsMovedToTools++;
        }

        Integer currentCount = toolInputsByMachine.get(machineId);
        if (currentCount == null) {
            toolInputsByMachine.put(machineId, 1);
        } else {
            toolInputsByMachine.put(machineId, currentCount + 1);
        }

        if (sampleToolInputs.size() < 25) {
            sampleToolInputs
                .add(machineId + ": " + toolStack.displayName + " [" + toolStack.id + ":" + toolStack.meta + "]");
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

    private boolean isProgrammedCircuit(ItemStack stack) {
        return "gregtech:gt.integrated_circuit".equals(itemStackExporter.getItemId(stack));
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

        identity.append("consumedItemInputs:");
        appendItemStacks(identity, recipe.mInputs, true, false);
        identity.append('|');

        identity.append("toolItemInputs:");
        appendItemStacks(identity, recipe.mInputs, true, true);
        identity.append('|');

        identity.append("fluidInputs:");
        appendFluidStacks(identity, recipe.mFluidInputs);
        identity.append('|');

        identity.append("itemOutputs:");
        appendItemStacks(identity, recipe.mOutputs, false, false);
        identity.append('|');

        identity.append("fluidOutputs:");
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

    private void appendItemStacks(StringBuilder identity, ItemStack[] stacks, boolean input, boolean toolsOnly) {
        if (stacks == null) {
            return;
        }

        for (ItemStack stack : stacks) {
            if (stack == null) {
                continue;
            }

            if (input && isProgrammedCircuit(stack)) {
                continue;
            }

            if (input) {
                boolean tool = isNonConsumedToolCandidate(stack);

                if (tool != toolsOnly) {
                    continue;
                }
            }

            identity.append("item:")
                .append(itemStackExporter.getItemId(stack))
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

}
