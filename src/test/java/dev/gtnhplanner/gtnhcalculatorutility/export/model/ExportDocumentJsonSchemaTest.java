package dev.gtnhplanner.gtnhcalculatorutility.export.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

public class ExportDocumentJsonSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void acceptsRepresentativeSchemaVersionTwoFixture() throws IOException {
        Schema schema = loadSchema();
        String documentJson = readResource("/fixtures/schema-v2-representative.json");

        List<com.networknt.schema.Error> errors = schema
            .validate(documentJson, InputFormat.JSON, executionContext -> {});

        assertTrue("Expected representative fixture to be valid, but received: " + errors, errors.isEmpty());
    }

    @Test
    public void rejectsUnsupportedSchemaVersion() throws IOException {
        Schema schema = loadSchema();

        JsonNode document = objectMapper.readTree(readResource("/fixtures/schema-v2-representative.json"));
        ((ObjectNode) document).put("schemaVersion", 3);

        List<com.networknt.schema.Error> errors = schema
            .validate(objectMapper.writeValueAsString(document), InputFormat.JSON, executionContext -> {});

        assertFalse("Expected unsupported schema version to be rejected", errors.isEmpty());
    }

    private Schema loadSchema() throws IOException {
        Path schemaPath = Paths.get("schema", "recipes-v2.schema.json");
        assertTrue("Missing JSON schema: " + schemaPath.toAbsolutePath(), Files.exists(schemaPath));

        String schemaData = new String(Files.readAllBytes(schemaPath), StandardCharsets.UTF_8);

        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

        return registry.getSchema(schemaData, InputFormat.JSON);
    }

    private String readResource(String resourcePath) throws IOException {
        InputStream inputStream = ExportDocumentJsonSchemaTest.class.getResourceAsStream(resourcePath);
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
