package dev.gtnhplanner.gtnhcalculatorutility.export.model;

import java.util.ArrayList;
import java.util.List;

public class ExportDocument {

    public int schemaVersion = 1;
    public PackInfo pack = new PackInfo();
    public ExportInfo export = new ExportInfo();
    public List<ExportRecipe> recipes = new ArrayList<>();

}
