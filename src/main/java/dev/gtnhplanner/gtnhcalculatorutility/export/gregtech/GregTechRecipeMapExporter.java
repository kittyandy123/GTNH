package dev.gtnhplanner.gtnhcalculatorutility.export.gregtech;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportStack;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.MachineInfo;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

public class GregTechRecipeMapExporter {

    private final Set<String> displayNameWarningKeys = new HashSet<>();

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
            getSafeDisplayName(stack),
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

    private String getSafeDisplayName(ItemStack stack) {
        try {
            String displayName = stack.getDisplayName();

            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            }
        } catch (Exception e) {
           String warningKey = getItemId(stack) + ":" + stack.getItemDamage();

           if (displayNameWarningKeys.add(warningKey)) {
               System.out.println("GTNH Calculator Utility could not read display name for item "
                   + warningKey
                   + " - "
                   + e.getClass().getSimpleName());
           }
        }
        return getItemId(stack) + ":" + stack.getItemDamage();
    }

    private String getItemId(ItemStack stack) {

        Item item = stack.getItem();
        String itemId = Item.itemRegistry.getNameForObject(item);

        if (itemId == null) {
            return "unknown";
        }

        return itemId;

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

}
