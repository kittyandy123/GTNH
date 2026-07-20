package dev.gtnhplanner.gtnhcalculatorutility.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportRecipe;

public class RecipePlanningClassifierTest {

    private final RecipePlanningClassifier classifier = new RecipePlanningClassifier();

    @Test
    public void leavesPositiveDurationRecipePlannableByDefault() {
        ExportRecipe recipe = new ExportRecipe();
        recipe.durationTicks = 40;
        recipe.durationSeconds = 2.0;

        classifier.classify(recipe);

        assertNull(recipe.planning);
    }

    @Test
    public void marksZeroDurationRecipeUnsupported() {
        ExportRecipe recipe = new ExportRecipe();
        recipe.durationTicks = 0;
        recipe.durationSeconds = 0.0;

        classifier.classify(recipe);

        assertFalse(recipe.planning.supported);
        assertEquals(1, recipe.planning.issues.size());
        assertTrue(recipe.planning.issues.contains(RecipePlanningClassifier.ZERO_DURATION));
    }

    @Test
    public void marksNegativeDurationAsSuspectedOverflow() {
        ExportRecipe recipe = new ExportRecipe();
        recipe.durationTicks = -2147483569;
        recipe.durationSeconds = -107374178.45;

        classifier.classify(recipe);

        assertFalse(recipe.planning.supported);
        assertEquals(-2147483569, recipe.durationTicks);
        assertEquals(-107374178.45, recipe.durationSeconds, 0.0);

        assertTrue(recipe.planning.issues.contains(RecipePlanningClassifier.NEGATIVE_DURATION));

        assertTrue(recipe.planning.issues.contains(RecipePlanningClassifier.DURATION_OVERFLOW_SUSPECTED));
    }

}
