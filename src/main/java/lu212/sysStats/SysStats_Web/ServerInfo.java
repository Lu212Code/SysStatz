package lu212.sysStats.SysStats_Web;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerInfo {
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

    public ServerInfo(String name, int cpuPercent, double ramUsed, double ramTotal, int diskPercent, double storageUsed, double storageTotal, String status, String boottime, String sent, String recv, String dsent, String drecv) {
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
    }

    public void update(int cpuPercent, double ramUsed, double ramTotal, int diskPercent, double storageUsed, double storageTotal, String status, String boottime, String sent, String recv, String dsent, String drecv) {
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

        // boottime nur setzen, wenn noch nicht gesetzt (also null oder leer)
        if (this.boottime == null || this.boottime.isEmpty()) {
            this.boottime = boottime;
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
