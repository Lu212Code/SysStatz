package lu212.sysstatz.General;

import java.time.LocalDateTime;
import java.util.List;

import lu212.sysstatz.StatsServer.Server.ServerProcessInfo;

public class ProcessLogEntry {
    private final LocalDateTime timestamp;
    private final List<ServerProcessInfo> topProcesses;

    public ProcessLogEntry(LocalDateTime timestamp, List<ServerProcessInfo> topProcesses) {
        this.timestamp = timestamp;
        this.topProcesses = topProcesses;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<ServerProcessInfo> getTopProcesses() {
        return topProcesses;
    }
}
