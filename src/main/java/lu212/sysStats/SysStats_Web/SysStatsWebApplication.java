package lu212.sysStats.SysStats_Web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.github.lalyos.jfiglet.FigletFont;

import lu212.sysStats.General.AnomalyMonitor;
import lu212.sysStats.General.Logger;
import lu212.sysStats.General.Plugins;
import lu212.sysStats.StatsServer.Server;

@SpringBootApplication
@EnableScheduling
public class SysStatsWebApplication {

	public static String webserverport = "8080";
	public static String statsserverport;
	public static String theme;
	public static String ollamaIP;
	public static String twoFactorRequired;
	public static String clientPassword;
	public static String apiKey;
	
	private static ConfigurableApplicationContext context;
	
	public static void main(String[] args) {
	    if (args.length > 0 && args[0].equalsIgnoreCase("--cli")) {
	        lu212.sysStats.General.commandLine.startCli();
	        return;
	    }
		Logger.start();
		Logger.info("Starte SysStatz...");
		System.out.println("Starte SysStats...");
		Logger.info("Lese Konfiguration aus...");
		System.out.println("Lese Konfiguration aus...");
		config();
		Logger.info("Lade plugins...");
		System.out.println("Lade plugins...");
		Plugins.loadPlugins();
		System.out.println("----------Config----------");
		System.out.println("Webserver-Port: " + webserverport);
		System.out.println("Statsserver-Port: " + statsserverport);
		System.out.println("Website-Theme: " + theme);
		System.out.println("----------Config----------");
		Logger.info("Starte SysStats Webserver...");
		System.out.println("Starte SysStats Webserver...");
		try {
			Logger.info("Webserver wird auf Port " + webserverport + " gestartet.");
			System.out.println("Webserver wird auf Port " + webserverport + " gestartet.");
			context = SpringApplication.run(SysStatsWebApplication.class, args);
		} catch (Exception e) {
			Logger.error("Webserver konnte nicht gestartet werden:");
			System.err.println("Webserver konnte nicht gestartet werden:");
			e.printStackTrace();
			shutdown();
			System.exit(0);
		}
		AnomalyMonitor.start();
		Logger.info("Starte StatsServer...");
		System.out.println("Starte StatsServer...");
		
		String SysStatzLogo = null;
		try {
			SysStatzLogo = FigletFont.convertOneLine("SysStatz");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("--------------------------------------------------------");
		System.out.println(SysStatzLogo);
		System.out.println("-----------------------ver.-1.0-------------------------");
		
		try {
			Logger.info("Starte StatsServer");
			System.out.println("Starte StatsServer");
			Server.startServer(null);
		} catch (IOException e) {
			Logger.error("StatsServer konnte nicht gestartet werden:");
			System.err.println("StatsServer konnte nicht gestartet werden:");
			e.printStackTrace();
			shutdown();
			System.exit(0);
		}
	}
	
	
	private static void config() {
		ConfigManager config = new ConfigManager();
		
		webserverport = config.getWebServerPort();
		statsserverport = config.getStatsServerPort();
		theme = config.getTheme();
		ollamaIP = config.getOllamaServerIP();
		twoFactorRequired = config.getTwoFactor();
		clientPassword = config.getClientPassword();
		apiKey = config.getApiKey();
	}

	@Bean
	public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
	    return factory -> {
	        try {
	            int port = Integer.parseInt(webserverport);
	            factory.setPort(port);
	        } catch (NumberFormatException | NullPointerException e) {
	        	Logger.warning("⚠ Ungültiger Port: '" + webserverport + "' → Fallback auf 8080");
	            System.err.println("⚠ Ungültiger Port: '" + webserverport + "' → Fallback auf 8080");
	            factory.setPort(Integer.parseInt(webserverport));
	        }
	    };
	}
	
    public static void shutdown() {
        if (context != null) {
            context.close();
        }
    }
    
    public static void restart() {
        Thread thread = new Thread(() -> {
            context.close(); // alte App stoppen
            context = SpringApplication.run(SysStatsWebApplication.class);
        });

        thread.setDaemon(false);
        thread.start();
    }
}
