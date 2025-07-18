package lu212.sysStats.SysStats_Web;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

import com.github.lalyos.jfiglet.FigletFont;

import lu212.sysStats.StatsServer.Server;

@SpringBootApplication
public class SysStatsWebApplication {

	public static String webserverport = "8080";
	public static String statsserverport;
	public static String webpasswort;
	public static String theme;
	
	public static void main(String[] args) {
		System.out.println("Starte SysStats...");
		System.out.println("Lese Konfiguration aus...");
		config();
		System.out.println("----------Config----------");
		System.out.println("Webserver-Port: " + webserverport);
		System.out.println("Statsserver-Port: " + statsserverport);
		System.out.println("Website Passwort: " + webpasswort);
		System.out.println("----------Config----------");
		System.out.println("Starte SysStats Webserver...");
		try {
			System.out.println("Webserver wird auf Port " + webserverport + " gestartet.");
		SpringApplication.run(SysStatsWebApplication.class, args);
		} catch (Exception e) {
			System.err.println("Webserver konnte nicht gestartet werden:");
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Starte StatsServer...");
		
		String SysStatzLogo = null;
		try {
			SysStatzLogo = FigletFont.convertOneLine("SysStatz");
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("--------------------------------------------------------");
		System.out.println(SysStatzLogo);
		System.out.println("-----------------------ver.-0.1-------------------------");
		
		try {
			Server.main(null);
		} catch (IOException e) {
			System.err.println("StatsServer konnte nicht gestartet werden:");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private static void config() {
		ConfigManager config = new ConfigManager();
		
		webserverport = config.getWebServerPort();
		statsserverport = config.getStatsServerPort();
		webpasswort = config.getPassword();
		theme = config.getTheme();
	}

	@Bean
	public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
	    return factory -> {
	        try {
	            int port = Integer.parseInt(webserverport);
	            factory.setPort(port);
	        } catch (NumberFormatException | NullPointerException e) {
	            System.err.println("⚠ Ungültiger Port: '" + webserverport + "' → Fallback auf 8080");
	            factory.setPort(Integer.parseInt(webserverport));
	        }
	    };
	}
	
}
