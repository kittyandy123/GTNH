package dev.gtnhplanner.gtnhcalculatorutility.export.model;

import java.util.ArrayList;
import java.util.List;

public class ExportDocument {

    public int schemaVersion = 2;
    public PackInfo pack = new PackInfo();
    public ExportInfo export = new ExportInfo();
    public ExportDiagnostics diagnostics = new ExportDiagnostics();
    public List<ExportRecipe> recipes = new ArrayList<>();

}
