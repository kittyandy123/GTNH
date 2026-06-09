package dev.gtnhplanner.gtnhcalculatorutility.export.gregtech;

import java.util.ArrayList;
import java.util.List;

import gregtech.api.recipe.RecipeMaps;

public class GregTechRecipeMapCatalog {

    public List<GregTechRecipeMapDefinition> getRecipeMaps() {
        List<GregTechRecipeMapDefinition> recipeMaps = new ArrayList<>();

        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:mixer", "Mixer", RecipeMaps.mixerRecipes));
        recipeMaps
            .add(new GregTechRecipeMapDefinition("gregtech:centrifuge", "Centrifuge", RecipeMaps.centrifugeRecipes));
        recipeMaps.add(
            new GregTechRecipeMapDefinition("gregtech:electrolyzer", "Electrolyzer", RecipeMaps.electrolyzerRecipes));
        recipeMaps.add(
            new GregTechRecipeMapDefinition(
                "gregtech:chemical_reactor",
                "Chemical Reactor",
                RecipeMaps.chemicalReactorRecipes));
        recipeMaps
            .add(new GregTechRecipeMapDefinition("gregtech:distillery", "Distillery", RecipeMaps.distilleryRecipes));
        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:macerator", "Macerator", RecipeMaps.maceratorRecipes));
        recipeMaps
            .add(new GregTechRecipeMapDefinition("gregtech:compressor", "Compressor", RecipeMaps.compressorRecipes));
        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:extractor", "Extractor", RecipeMaps.extractorRecipes));
        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:bender", "Bender", RecipeMaps.benderRecipes));
        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:wiremill", "Wiremill", RecipeMaps.wiremillRecipes));
        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:lathe", "Lathe", RecipeMaps.latheRecipes));
        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:assembler", "Assembler", RecipeMaps.assemblerRecipes));
        recipeMaps.add(
            new GregTechRecipeMapDefinition(
                "gregtech:fluid_solidifier",
                "Fluid Solidifier",
                RecipeMaps.fluidSolidifierRecipes));
        recipeMaps.add(
            new GregTechRecipeMapDefinition("gregtech:forming_press", "Forming Press", RecipeMaps.formingPressRecipes));
        recipeMaps.add(
            new GregTechRecipeMapDefinition("gregtech:cutting_machine", "Cutting Machine", RecipeMaps.cutterRecipes));
        recipeMaps.add(
            new GregTechRecipeMapDefinition(
                "gregtech:laser_engraver",
                "Laser Engraver",
                RecipeMaps.laserEngraverRecipes));
        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:polarizer", "Polarizer", RecipeMaps.polarizerRecipes));
        recipeMaps.add(new GregTechRecipeMapDefinition("gregtech:extruder", "Extruder", RecipeMaps.extruderRecipes));

        return recipeMaps;
    }

}
