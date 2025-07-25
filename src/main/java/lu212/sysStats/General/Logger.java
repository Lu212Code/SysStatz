package lu212.sysStats.General;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

	private static LogManager logger = new LogManager("logs/log-" + getAktuellesDatumMitUhrzeit() + ".txt");
	
	public static void info(String text) {
		logger.info(text);
	}
	
	public static void error(String text) {
		logger.error(text);
	}
	
	public static void warning(String text) {
		logger.warning(text);
	}
	
	public static void start() {
		logger.initialize();
	}
	
	public static String getAktuellesDatumMitUhrzeit() {
	    LocalDateTime jetzt = LocalDateTime.now();
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm");
	    return jetzt.format(formatter);
	}
}
