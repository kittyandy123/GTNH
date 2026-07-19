package dev.gtnhplanner.gtnhcalculatorutility.export;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import dev.gtnhplanner.gtnhcalculatorutility.export.gregtech.GregTechRecipeMapCatalog;
import dev.gtnhplanner.gtnhcalculatorutility.export.gregtech.GregTechRecipeMapDefinition;
import dev.gtnhplanner.gtnhcalculatorutility.export.gregtech.GregTechRecipeMapExporter;
import dev.gtnhplanner.gtnhcalculatorutility.export.item.ItemStackExporter;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDiagnostics;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;
import dev.gtnhplanner.gtnhcalculatorutility.export.vanilla.VanillaFurnaceExporter;

public class RecipeExporter {

    private final ExportDocumentJsonWriter jsonWriter = new ExportDocumentJsonWriter();
    private final RecipePlanningClassifier planningClassifier = new RecipePlanningClassifier();

    private int duplicateRecipesSkipped;

    public ExportResult exportRecipes(File minecraftDir) throws IOException {
        duplicateRecipesSkipped = 0;

        File exportDir = new File(minecraftDir, "gtnh-calculator-utility");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IOException("Failed to create export directory: " + exportDir.getAbsolutePath());
        }

        ExportDocument document = new ExportDocument();
        document.export.exportedAt = createUtcTimestamp();

        ItemStackExporter itemStackExporter = new ItemStackExporter();

        new VanillaFurnaceExporter(itemStackExporter).addRecipes(document);

        GregTechRecipeMapExporter gregTechExporter = new GregTechRecipeMapExporter(itemStackExporter);
        GregTechRecipeMapCatalog gregTechCatalog = new GregTechRecipeMapCatalog();

        for (GregTechRecipeMapDefinition definition : gregTechCatalog.getRecipeMaps()) {
            try {
                System.out.println("GTNH Calculator Utility exporting map: " + definition.machineId);

                gregTechExporter
                    .addRecipeMap(document, definition.machineId, definition.machineName, definition.recipeMap);
            } catch (Exception e) {
                System.out.println("GTNH Calculator Utility failed to export map: " + definition.machineId);
                e.printStackTrace();
            }
        }

        deduplicateRecipes(document);
        planningClassifier.classify(document);
        populateDiagnostics(document, itemStackExporter, gregTechExporter);

        File outputFile = new File(exportDir, "recipes.json");
        jsonWriter.write(outputFile, document);

        return createResult(outputFile, document);
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

    private void populateDiagnostics(ExportDocument document, ItemStackExporter itemStackExporter,
        GregTechRecipeMapExporter gregTechExporter) {
        ExportDiagnostics diagnostics = new ExportDiagnostics();

        diagnostics.totalRecipes = document.recipes.size();
        diagnostics.duplicateRecipesSkipped = duplicateRecipesSkipped;
        diagnostics.displayNameFallbackItems.addAll(itemStackExporter.getDisplayNameFallbackItems());
        diagnostics.displayNameFallbacks = diagnostics.displayNameFallbackItems.size();
        diagnostics.recipesSkippedDueToError = gregTechExporter.getRecipesSkippedDueToError();
        diagnostics.recipeErrorsByMachine.putAll(gregTechExporter.getRecipeErrorsByMachine());

        diagnostics.toolInputsExtracted = gregTechExporter.getToolInputsExtracted();
        diagnostics.toolInputsByMachine.putAll(gregTechExporter.getToolInputsByMachine());
        diagnostics.zeroAmountInputsMovedToTools = gregTechExporter.getZeroAmountInputsMovedToTools();
        diagnostics.zeroAmountInputsRemaining = gregTechExporter.getZeroAmountInputsRemaining();
        diagnostics.inferredToolAmounts = gregTechExporter.getInferredToolAmounts();
        diagnostics.sampleToolInputs.addAll(gregTechExporter.getSampleToolInputs());

        populatePlanningDiagnostics(diagnostics, document);

        diagnostics.recipeCountsByMachine.putAll(countRecipesByMachine(document));

        document.diagnostics = diagnostics;
    }

    private void populatePlanningDiagnostics(ExportDiagnostics diagnostics, ExportDocument document) {
        for (ExportRecipe recipe : document.recipes) {
            if (recipe.planning == null || recipe.planning.supported) {
                continue;
            }

            diagnostics.nonPlannableRecipes++;

            String machineId = "unknown";

            if (recipe.machine != null && recipe.machine.id != null) {
                machineId = recipe.machine.id;
            }

            incrementCount(diagnostics.nonPlannableRecipesByMachine, machineId);

            if (recipe.durationTicks <= 0 || recipe.durationSeconds <= 0) {
                diagnostics.nonPositiveDurationRecipes++;
            }

            if (recipe.planning.issues.contains(RecipePlanningClassifier.DURATION_OVERFLOW_SUSPECTED)) {
                diagnostics.suspectedDurationOverflowRecipes++;
            }

            if (diagnostics.sampleNonPlannableRecipes.size() < 25) {
                diagnostics.sampleNonPlannableRecipes.add(
                    machineId + ": "
                        + recipe.id
                        + " [ticks="
                        + recipe.durationTicks
                        + ", issues="
                        + recipe.planning.issues
                        + "]");
            }
        }
    }

    private void incrementCount(Map<String, Integer> counts, String key) {
        Integer currentCount = counts.get(key);

        if (currentCount == null) {
            counts.put(key, 1);
        } else {
            counts.put(key, currentCount + 1);
        }
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

}
