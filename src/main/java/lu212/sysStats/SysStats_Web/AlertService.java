package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertService {
	private final Path alertsFile = Paths.get("alerts.txt");

    public synchronized List<Alert> getAllAlerts() throws IOException {
        if (!Files.exists(alertsFile)) {
            return Collections.emptyList();
        }
        return Files.lines(alertsFile)
            .map(Alert::fromLine)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public synchronized void addAlert(String text, Alert.Level level) throws IOException {
        String line = level.name() + ";" + text.replace("\n", " ").replace(";", ",");
        Files.write(alertsFile, Collections.singleton(line), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public synchronized void deleteAlert(Alert alertToDelete) throws IOException {
        if (!Files.exists(alertsFile)) return;

        List<String> remaining = Files.lines(alertsFile)
            .filter(line -> {
                Alert a = Alert.fromLine(line);
                return a == null || !a.equals(alertToDelete);
            })
            .collect(Collectors.toList());

        Files.write(alertsFile, remaining, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public static class Alert {
        public enum Level { RED, YELLOW }
        public final String text;
        public final Level level;

        public Alert(Level level, String text) {
            this.level = level;
            this.text = text;
        }

        public static Alert fromLine(String line) {
            String[] parts = line.split(";", 2);
            if (parts.length < 2) return null;
            Level lvl;
            try {
                lvl = Level.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                return null;
            }
            return new Alert(lvl, parts[1]);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Alert)) return false;
            Alert other = (Alert) o;
            return this.level == other.level && this.text.equals(other.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, text);
        }
    }
}
