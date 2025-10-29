package lu212.sysstatz.General;

public class RegisteredValue {
    public String pluginName;   // z.B. CpuTempSensor
    public String displayName;  // z.B. CPU Temperatur
    public String valueKey;     // z.B. temp
    public String unit;         // z.B. °C

    public RegisteredValue(String pluginName, String displayName, String valueKey, String unit) {
        this.pluginName = pluginName;
        this.displayName = displayName;
        this.valueKey = valueKey;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return displayName + " (" + unit + ") from " + pluginName;
    }
}
