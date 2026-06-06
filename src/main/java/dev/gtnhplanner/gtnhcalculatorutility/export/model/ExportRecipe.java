package dev.gtnhplanner.gtnhcalculatorutility.export.model;

import java.util.ArrayList;
import java.util.List;

public class ExportRecipe {

    public String id;
    public MachineInfo machine;
    public int durationTicks;
    public double durationSeconds;
    public int eut;
    public List<ExportStack> inputs = new ArrayList<>();
    public List<ExportStack> outputs = new ArrayList<>();
    public RecipeMetadata metadata = new RecipeMetadata();

}
