package lu212.sysStats.SysStats_Web;

import java.time.LocalDateTime;
import java.util.Map;

public class ServerHistoryEntry {
    private final LocalDateTime timestamp;
    private final int cpuPercent;
    private final double ramUsed;
    private final int diskPercent;
    private final String dsent;
    private final String drecv;
    private double swapUsed;
    private double swapTotal;
    private Map<Integer, Double> CpuCoreLoad;


    public ServerHistoryEntry(LocalDateTime timestamp, int cpuPercent, double ramUsed, int diskPercent, String dsent, String drecv, double SwapTotal, double SwapUsed, Map<Integer, Double> CpuCoreLoad) {
        this.timestamp = timestamp;
        this.cpuPercent = cpuPercent;
        this.ramUsed = ramUsed;
        this.diskPercent = diskPercent;
        this.dsent = dsent;
        this.drecv = drecv;
        this.swapUsed = SwapUsed;
        this.swapTotal = SwapTotal;
        this.CpuCoreLoad = CpuCoreLoad;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getCpuPercent() { return cpuPercent; }
    public double getRamUsed() { return ramUsed; }
    public int getDiskPercent() { return diskPercent; }
    public String getDsent() { return dsent; }
    public String getDrecv() { return drecv; }
    public double getSwapUsed() { return swapUsed; }
    public double getSwapTotal() { return swapTotal; }
    public Map<Integer, Double> getCpuCoreLoad() { return CpuCoreLoad; }
}
