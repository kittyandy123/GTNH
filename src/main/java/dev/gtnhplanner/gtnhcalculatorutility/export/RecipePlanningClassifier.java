package dev.gtnhplanner.gtnhcalculatorutility.export;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;
import dev.gtnhplanner.gtnhcalculatorutility.export.model.RecipePlanningInfo;

public class RecipePlanningClassifier {

    public static final String NEGATIVE_DURATION = "negative-duration";
    public static final String ZERO_DURATION = "zero-duration";
    public static final String DURATION_OVERFLOW_SUSPECTED = "duration-overflow-suspected";

    public void classify(ExportDocument document) {
        for (ExportRecipe recipe : document.recipes) {
            classify(recipe);
        }
    }

    public void classify(ExportRecipe recipe) {
        if (recipe.durationTicks < 0 || recipe.durationSeconds < 0) {
            addIssue(recipe, NEGATIVE_DURATION);

            if (recipe.durationTicks < 0) {
                addIssue(recipe, DURATION_OVERFLOW_SUSPECTED);
            }

            return;
        }

        if (recipe.durationTicks == 0 || recipe.durationSeconds == 0) {
            addIssue(recipe, ZERO_DURATION);
        }
    }

    private void addIssue(ExportRecipe recipe, String issue) {
        if (recipe.planning == null) {
            recipe.planning = new RecipePlanningInfo();
        }

        recipe.planning.supported = false;

        if (!recipe.planning.issues.contains(issue)) {
            recipe.planning.issues.add(issue);
        }
    }

}
