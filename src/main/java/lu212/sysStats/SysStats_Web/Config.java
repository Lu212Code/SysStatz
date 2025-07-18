package lu212.sysStats.SysStats_Web;

public class Config {
    private String webPort;
    private String statsPort;
    private String password;
    private String theme;
    private String updateRate;

    // Getter und Setter
    public String getWebPort() { return webPort; }
    public void setWebPort(String webPort) { this.webPort = webPort; }

    public String getStatsPort() { return statsPort; }
    public void setStatsPort(String statsPort) { this.statsPort = statsPort; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getUpdateRate() { return updateRate; }
    public void setUpdateRate(String updateRate) { this.updateRate = updateRate; }
}
