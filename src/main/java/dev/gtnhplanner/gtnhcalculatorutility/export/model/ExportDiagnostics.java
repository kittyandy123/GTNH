package dev.gtnhplanner.gtnhcalculatorutility.export.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExportDiagnostics {

    public int totalRecipes;
    public int duplicateRecipesSkipped;
    public Map<String, Integer> recipeCountsByMachine = new LinkedHashMap<>();

}
