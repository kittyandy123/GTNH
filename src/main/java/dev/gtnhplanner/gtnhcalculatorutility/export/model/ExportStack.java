package dev.gtnhplanner.gtnhcalculatorutility.export.model;

public class ExportStack {

    public String kind;
    public String id;
    public int meta;
    public String displayName;
    public int amount;
    public String unit;
    public Double chance;

    public ExportStack(String kind, String id, int meta, String displayName, int amount, String unit) {
        this.kind = kind;
        this.id = id;
        this.meta = meta;
        this.displayName = displayName;
        this.amount = amount;
        this.unit = unit;
    }

}
