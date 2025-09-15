package lu212.sysStats.SysStats_Web;

public class Config {
    private String webPort;
    private String statsPort;
    private String theme;
    private String ollamaserverip;
    private String twoFactorRequired;
    private String clientPassword;
    private String apiKey;

    // Getter und Setter
    public String getWebPort() { return webPort; }
    public void setWebPort(String webPort) { this.webPort = webPort; }

    public String getStatsPort() { return statsPort; }
    public void setStatsPort(String statsPort) { this.statsPort = statsPort; }
    
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    
    public String getOllamaserverip() { return ollamaserverip; }
    public void setOllamaserverip(String ollamaserverip) { this.ollamaserverip = ollamaserverip; }
    
    public String getTwoFactorRequired() { return twoFactorRequired; }
    public void setTwoFactorRequired(String twoFactorRequired) { this.twoFactorRequired = twoFactorRequired; }
    
    public String getClientPassword() { return clientPassword; }
    public void setClientPassword(String clientPassword) { this.clientPassword = clientPassword; }
    
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }	
    }