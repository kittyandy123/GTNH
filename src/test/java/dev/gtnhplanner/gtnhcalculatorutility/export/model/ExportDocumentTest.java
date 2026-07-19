package dev.gtnhplanner.gtnhcalculatorutility.export.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.gtnhplanner.gtnhcalculatorutility.export.ExportDocumentJsonWriter;

public class ExportDocumentTest {

    @Test
    public void initializesSchemaVersionTwoDocument() {
        ExportDocument document = new ExportDocument();

        assertEquals(2, document.schemaVersion);
        assertNotNull(document.pack);
        assertNotNull(document.export);
        assertNotNull(document.diagnostics);
        assertNotNull(document.recipes);
        assertTrue(document.recipes.isEmpty());
    }

    @Test
    public void serializesRepresentativeSchemaVersionTwoRecipe() {
        ExportDocument document = createRepresentativeDocument();

        StringWriter writer = new StringWriter();
        new ExportDocumentJsonWriter().write(writer, document);

        JsonObject root = new JsonParser().parse(writer.toString())
            .getAsJsonObject();

        assertEquals(
            2,
            root.get("schemaVersion")
                .getAsInt());

        JsonArray recipes = root.getAsJsonArray("recipes");
        assertEquals(1, recipes.size());

        JsonObject serializedRecipe = recipes.get(0)
            .getAsJsonObject();

        assertEquals(
            "gregtech:mixer:test_recipe",
            serializedRecipe.get("id")
                .getAsString());
        assertEquals(
            "gregtech:mixer",
            serializedRecipe.getAsJsonObject("machine")
                .get("id")
                .getAsString());
        assertEquals(
            40,
            serializedRecipe.get("durationTicks")
                .getAsInt());
        assertEquals(
            2.0,
            serializedRecipe.get("durationSeconds")
                .getAsDouble(),
            0.0);
        assertEquals(
            30,
            serializedRecipe.get("eut")
                .getAsInt());

        JsonObject serializedInput = serializedRecipe.getAsJsonArray("inputs")
            .get(0)
            .getAsJsonObject();
        assertEquals(
            "item",
            serializedInput.get("kind")
                .getAsString());
        assertEquals(
            "minecraft:clay_ball",
            serializedInput.get("id")
                .getAsString());
        assertEquals(
            2,
            serializedInput.get("amount")
                .getAsInt());
        assertEquals(
            "items",
            serializedInput.get("unit")
                .getAsString());
        assertFalse(serializedInput.has("chance"));

        JsonObject serializedTool = serializedRecipe.getAsJsonArray("tools")
            .get(0)
            .getAsJsonObject();
        assertEquals(
            "gregtech:shape_mold_plate",
            serializedTool.get("id")
                .getAsString());
        assertEquals(
            1,
            serializedTool.get("amount")
                .getAsInt());

        JsonObject serializedOutput = serializedRecipe.getAsJsonArray("outputs")
            .get(0)
            .getAsJsonObject();
        assertEquals(
            "fluid",
            serializedOutput.get("kind")
                .getAsString());
        assertEquals(
            "water",
            serializedOutput.get("id")
                .getAsString());
        assertEquals(
            1000,
            serializedOutput.get("amount")
                .getAsInt());
        assertEquals(
            "L",
            serializedOutput.get("unit")
                .getAsString());
        assertEquals(
            0.75,
            serializedOutput.get("chance")
                .getAsDouble(),
            0.0);

        assertEquals(
            24,
            serializedRecipe.getAsJsonObject("metadata")
                .get("circuit")
                .getAsInt());
    }

    @Test
    public void matchesRepresentativeSchemaVersionTwoFixture() throws IOException {
        ExportDocument document = createRepresentativeDocument();

        StringWriter writer = new StringWriter();
        new ExportDocumentJsonWriter().write(writer, document);

        JsonElement expected = new JsonParser().parse(readResource("/fixtures/schema-v2-representative.json"));
        JsonElement actual = new JsonParser().parse(writer.toString());

        assertEquals(expected, actual);
    }

    private ExportDocument createRepresentativeDocument() {
        ExportDocument document = new ExportDocument();
        document.export.exportedAt = "2026-07-19T00:00:00Z";

        ExportRecipe recipe = new ExportRecipe();
        recipe.id = "gregtech:mixer:test_recipe";
        recipe.machine = new MachineInfo("gregtech:mixer", "Mixer", "GregTech");
        recipe.durationTicks = 40;
        recipe.durationSeconds = 2.0;
        recipe.eut = 30;

        ExportStack input = new ExportStack("item", "minecraft:clay_ball", 0, "Clay", 2, "items");
        recipe.inputs.add(input);

        recipe.tools = new ArrayList<>();
        ExportStack tool = new ExportStack("item", "gregtech:shape_mold_plate", 0, "Plate Mold", 1, "items");
        recipe.tools.add(tool);

        ExportStack output = new ExportStack("fluid", "water", 0, "Water", 1000, "L");
        output.chance = 0.75;
        recipe.outputs.add(output);

        recipe.metadata.circuit = 24;
        document.recipes.add(recipe);

        document.diagnostics.totalRecipes = 1;
        document.diagnostics.toolInputsExtracted = 1;
        document.diagnostics.toolInputsByMachine.put("gregtech:mixer", 1);
        document.diagnostics.sampleToolInputs.add("gregtech:mixer: Plate Mold [gregtech:shape_mold_plate:0]");
        document.diagnostics.recipeCountsByMachine.put("gregtech:mixer", 1);

        return document;
    }

    private String readResource(String resourcePath) throws IOException {
        InputStream inputStream = ExportDocumentTest.class.getResourceAsStream(resourcePath);
        assertNotNull("Missing test resource: " + resourcePath, inputStream);

        StringBuilder content = new StringBuilder();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int charactersRead;

            while ((charactersRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, charactersRead);
            }
        }

        return content.toString();
    }

}
