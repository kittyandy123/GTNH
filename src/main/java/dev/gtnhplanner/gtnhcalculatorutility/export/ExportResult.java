package dev.gtnhplanner.gtnhcalculatorutility.export;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExportResult {

    public final File outputFile;
    public final int totalRecipes;
    public final int duplicateRecipesSkipped;
    public final Map<String, Integer> recipeCountByMachine;

    public ExportResult(File outputFile, int totalRecipes, int duplicateRecipesSkipped,
        Map<String, Integer> recipeCountsByMachine) {
        this.outputFile = outputFile;
        this.totalRecipes = totalRecipes;
        this.duplicateRecipesSkipped = duplicateRecipesSkipped;
        this.recipeCountByMachine = new LinkedHashMap<>(recipeCountsByMachine);
    }

}
