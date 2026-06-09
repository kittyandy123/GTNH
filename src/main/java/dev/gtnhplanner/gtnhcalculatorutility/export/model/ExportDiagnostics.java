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
    public Map<String, Integer> recipeCountsByMachine = new LinkedHashMap<>();

}
