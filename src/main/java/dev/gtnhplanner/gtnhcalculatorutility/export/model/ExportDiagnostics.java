package dev.gtnhplanner.gtnhcalculatorutility.export.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportDiagnostics {

    public int totalRecipes;
    public int duplicateRecipesSkipped;
    public int displayNameFallbacks;
    public List<String> displayNameFallbackItems = new ArrayList<>();
    public int recipesSkippedDueToError;
    public Map<String, Integer> recipeErrorsByMachine = new LinkedHashMap<>();

    public int toolInputsExtracted;
    public Map<String, Integer> toolInputsByMachine = new LinkedHashMap<>();
    public int zeroAmountInputsMovedToTools;
    public int zeroAmountInputsRemaining;
    public int inferredToolAmounts;
    public List<String> sampleToolInputs = new ArrayList<>();

    public int nonPlannableRecipes;
    public Map<String, Integer> nonPlannableRecipesByMachine = new LinkedHashMap<>();
    public int nonPositiveDurationRecipes;
    public int suspectedDurationOverflowRecipes;
    public int suspectedSentinelDurationRecipes;
    public List<String> sampleNonPlannableRecipes = new ArrayList<>();

    public Map<String, Integer> recipeCountsByMachine = new LinkedHashMap<>();

}
