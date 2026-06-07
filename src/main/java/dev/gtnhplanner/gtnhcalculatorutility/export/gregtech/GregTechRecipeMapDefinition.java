package dev.gtnhplanner.gtnhcalculatorutility.export.gregtech;

import gregtech.api.recipe.RecipeMap;

public class GregTechRecipeMapDefinition {

    public final String machineId;
    public final String machineName;
    public final RecipeMap<?> recipeMap;

    public GregTechRecipeMapDefinition(String machineId, String machineName, RecipeMap<?> recipeMap) {
        this.machineId = machineId;
        this.machineName = machineName;
        this.recipeMap = recipeMap;
    }

}
