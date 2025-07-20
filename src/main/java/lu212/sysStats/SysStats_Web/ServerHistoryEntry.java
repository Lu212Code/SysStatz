package lu212.sysStats.SysStats_Web;

import java.time.LocalDateTime;

public class ServerHistoryEntry {
    private final LocalDateTime timestamp;
    private final int cpuPercent;
    private final double ramUsed;
    private final int diskPercent;
    private final String dsent;
    private final String drecv;

    public ServerHistoryEntry(LocalDateTime timestamp, int cpuPercent, double ramUsed, int diskPercent, String dsent, String drecv) {
        this.timestamp = timestamp;
        this.cpuPercent = cpuPercent;
        this.ramUsed = ramUsed;
        this.diskPercent = diskPercent;
        this.dsent = dsent;
        this.drecv = drecv;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getCpuPercent() { return cpuPercent; }
    public double getRamUsed() { return ramUsed; }
    public int getDiskPercent() { return diskPercent; }
    public String getDsent() { return dsent; }
    public String getDrecv() { return drecv; }
}
