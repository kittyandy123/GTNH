package dev.gtnhplanner.gtnhcalculatorutility.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.gtnhplanner.gtnhcalculatorutility.export.model.ExportDocument;

public class ExportDocumentJsonWriter {

    private final Gson gson = new GsonBuilder().setPrettyPrinting()
        .create();

    public void write(File outputFile, ExportDocument document) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            write(writer, document);
        }
    }

    public void write(Writer writer, ExportDocument document) {
        gson.toJson(document, writer);
    }

}
