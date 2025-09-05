package lu212.sysStats.SysStats_Web;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import lu212.sysStats.StatsServer.Server.ServerProcessInfo;

public class ServerInfo {
	
	private final List<ServerHistoryEntry> history = new ArrayList<>();
	
    private final String name;
    private int cpuPercent;
    private double ramUsed;
    private double ramTotal;
    private int diskPercent;
    private double storageUsed;
    private double storageTotal;
    private String status;
    private String boottime;
    private String uptime;
    private String sent;
    private String recv;
    private String dsent;
    private String drecv;
    private List<ServerProcessInfo> processes = new ArrayList<>();
    private String scmd;
    private String temp;
    private Map<Integer, Double> cpuCoreLoads;
    private double swapUsed;
    private double swapTotal;
    private String cpuVoltage;
    private String loadavg1;
    private String loadavg5;
    private String loadavg15;
    private Map<Integer, Double> cpuCoreFreqs;

    public ServerInfo(String name, int cpuPercent, double ramUsed, double ramTotal, int diskPercent, double storageUsed, double storageTotal,
            String status, String boottime, String sent, String recv, String dsent, String drecv,
            List<ServerProcessInfo> processes, String scmd, String temp, Map<Integer, Double> cpuCoreLoads,
            String swapTotal, String swapUsed, Map<Integer, Double> cpuCoreFreqs) {
this.name = name;
this.cpuPercent = cpuPercent;
this.ramUsed = ramUsed;
this.ramTotal = ramTotal;
this.diskPercent = diskPercent;
this.storageUsed = storageUsed;
this.storageTotal = storageTotal;
this.status = status;
this.boottime = boottime;
this.uptime = berechneUptime(this.boottime);
this.sent = sent;
this.recv = recv;
this.dsent = dsent;
this.drecv = drecv;
this.processes = processes != null ? processes : new ArrayList<>();
this.scmd = scmd;
this.temp = temp;
this.cpuCoreLoads = cpuCoreLoads;
this.cpuCoreFreqs = cpuCoreFreqs;

// Sicheres Parsen von Swap-Werten
try {
  this.swapTotal = Double.parseDouble(swapTotal);
} catch (Exception e) {
  this.swapTotal = 0;
}
try {
  this.swapUsed = Double.parseDouble(swapUsed);
} catch (Exception e) {
  this.swapUsed = 0;
}
}

    public void update(int cpuPercent, double ramUsed, double ramTotal, int diskPercent, double storageUsed,
            double storageTotal, String status, String boottime, String sent, String recv, String dsent, String drecv,
            List<ServerProcessInfo> processes, String scmd, String temp, Map<Integer, Double> cpuCoreLoads,
            String swapTotal, String swapUsed,
            Map<Integer, Double> cpuCoreFreqs) {
        this.cpuPercent = cpuPercent;
        this.ramUsed = ramUsed;
        this.ramTotal = ramTotal;
        this.diskPercent = diskPercent;
        this.storageUsed = storageUsed;
        this.storageTotal = storageTotal;
        this.status = status;
        this.uptime = berechneUptime(this.boottime);
        this.sent = sent;
        this.recv = recv;
        this.dsent = dsent;
        this.drecv = drecv;
        this.processes = processes != null ? processes : new ArrayList<>();
        this.scmd = scmd;
        this.temp = temp;
        this.cpuCoreLoads = cpuCoreLoads;
        this.cpuCoreFreqs = cpuCoreFreqs;
        try {
        this.swapTotal = Double.parseDouble(swapTotal);
        this.swapUsed = Double.parseDouble(swapUsed);
        } catch (Exception e) {
        	e.printStackTrace();
        }

        // boottime nur setzen, wenn noch nicht gesetzt (also null oder leer)
        if (this.boottime == null || this.boottime.isEmpty()) {
            this.boottime = boottime;
        }
        
     // Neue Verlaufsdaten hinzufügen
        history.add(new ServerHistoryEntry(
            LocalDateTime.now(),
            cpuPercent,
            ramUsed,
            diskPercent,
            dsent,
            drecv,
            this.swapTotal,
            this.swapUsed,
            cpuCoreLoads
        ));

        // 7 Tage alte Daten entfernen
        LocalDateTime eineWocheZurueck = LocalDateTime.now().minusDays(7);
        Iterator<ServerHistoryEntry> iterator = history.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getTimestamp().isBefore(eineWocheZurueck)) {
                iterator.remove();
            }
        }

        
    }


    // Getter
    public String getName() { return name; }
    public int getCpuPercent() { return cpuPercent; }
    public double getRamUsed() { return ramUsed; }
    public double getRamTotal() { return ramTotal; }
    public int getDiskPercent() { return diskPercent; }
    public double getStorageUsed() { return storageUsed; }
    public double getStorageTotal() { return storageTotal; }
    public String getStatus() { return status; }
    public String getBoottime() { return boottime; }
    public String getUptime() { return uptime; }
    public String getSent() { return sent; }
    public String getRecv() { return recv; }
    public String getDsend() { return dsent; }
    public String getDrecv() { return drecv; }
    public List<ServerProcessInfo> getProcesses() { return processes; }
    public List<ServerHistoryEntry> getHistory() { return history; }
    public String getScmd() { return scmd; }
    public String getTemp() { return temp; }
    public Map<Integer, Double> getCpuCore() { return cpuCoreLoads; }
	public double getSwapUsed() { return swapUsed; }
	public double getSwapTotal() { return swapTotal; }
	public Map<Integer, Double> getCpuCoreFreqs() { return cpuCoreFreqs; }

    
    public static String berechneUptime(String boottimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        LocalDateTime boottime = LocalDateTime.parse(boottimeStr, formatter);
        LocalDateTime jetzt = LocalDateTime.now();

        Duration duration = Duration.between(boottime, jetzt);

        long tage = duration.toDays();
        long stunden = duration.toHours() % 24;
        long minuten = duration.toMinutes() % 60;
        long sekunden = duration.getSeconds() % 60;

        return String.format("%d Tage, %02d:%02d:%02d", tage, stunden, minuten, sekunden);
    }
}
