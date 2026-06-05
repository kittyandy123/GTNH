package dev.gtnhplanner.gtnhcalculatorutility.gregtech;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;

public class GregTechRecipeMapSummary {

    public List<String> summarizeRecipeMaps() {
        List<String> lines = new ArrayList<>();

        Field[] fields = RecipeMaps.class.getFields();

        for (Field field : fields) {
            if (!RecipeMap.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                Object recipeMap = field.get(null);
                if (recipeMap == null) {
                    continue;
                }

                int recipeCount = getRecipeCount(recipeMap);
                lines.add(field.getName() + ": " + recipeCount);
            } catch (IllegalAccessException e) {
                lines.add(field.getName() + ": inaccessible");
            }
        }

        if (lines.isEmpty()) {
            lines.add("No GregTech RecipeMaps were found.");
        }

        return lines;
    }

    private int getRecipeCount(Object recipeMap) {
        Collection<?> recipes = tryGetRecipesByMethod(recipeMap, "getAllRecipes");
        if (recipes != null) {
            return recipes.size();
        }

        recipes = tryGetRecipesByMethod(recipeMap, "getRecipeList");
        if (recipes != null) {
            return recipes.size();
        }

        recipes = tryGetRecipesByField(recipeMap, "mRecipeList");
        if (recipes != null) {
            return recipes.size();
        }

        recipes = tryGetRecipesByField(recipeMap, "recipeList");
        if (recipes != null) {
            return recipes.size();
        }

        return -1;
    }

    private Collection<?> tryGetRecipesByMethod(Object target, String methodName) {
        try {
            Method method = target.getClass()
                .getMethod(methodName);

            Object value = method.invoke(target);
            if (value instanceof Collection) {
                return (Collection<?>) value;
            }
        } catch (Exception ignored) {

        }

        return null;
    }

    private Collection<?> tryGetRecipesByField(Object target, String fieldName) {
        Class<?> currentClass = target.getClass();

        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);

                Object value = field.get(target);
                if (value instanceof Collection) {
                    return (Collection<?>) value;
                }
            } catch (Exception ignored) {

            }

            currentClass = currentClass.getSuperclass();
        }

        return null;
    }

}
