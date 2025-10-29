package lu212.sysstatz.General;

import java.io.*;

public class Restart {

    private static final String RESTART_LINUX = "restart.sh";
    private static final String RESTART_WINDOWS = "restart.bat";

    // Arbeitsverzeichnis, in dem die JAR liegt
    private static final String WORK_DIR = "."; // aktuelles Verzeichnis

    public static void restartSyStatz() throws IOException, InterruptedException {
        String jarName = findJarName();
        if (jarName == null) {
            throw new FileNotFoundException("Keine SyStatz-JAR im Verzeichnis gefunden!");
        }

        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            createWindowsScriptIfNotExists(jarName);
            executeWindowsScript();
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            createLinuxScriptIfNotExists(jarName);
            executeLinuxScript();
        } else {
            throw new UnsupportedOperationException("OS nicht unterstützt: " + os);
        }
    }

    private static String findJarName() {
        File dir = new File(WORK_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (files != null && files.length > 0) {
            // Nimm einfach die erste JAR im Verzeichnis
            return files[0].getName();
        }
        return null;
    }

    private static void createWindowsScriptIfNotExists(String jarName) throws IOException {
        File script = new File(RESTART_WINDOWS);
        if (!script.exists()) {
            try (PrintWriter writer = new PrintWriter(script)) {
                writer.println("@echo off");
                writer.println("echo Neustarte SyStatz...");
                writer.println("taskkill /F /IM java.exe"); // killt laufende Java Prozesse
                writer.println("timeout /t 2"); // 2 Sekunden warten
                writer.println("start java -jar \"" + jarName + "\"");
            }
        }
    }

    private static void createLinuxScriptIfNotExists(String jarName) throws IOException {
        File script = new File(RESTART_LINUX);
        if (!script.exists()) {
            try (PrintWriter writer = new PrintWriter(script)) {
                writer.println("#!/bin/bash");
                writer.println("echo \"Neustarte SyStatz...\"");
                writer.println("pkill -f \"" + jarName + "\""); // killt laufende JAR
                writer.println("sleep 2"); // 2 Sekunden warten
                writer.println("nohup java -jar \"" + jarName + "\" &"); // neu starten
            }
            script.setExecutable(true);
        }
    }

    private static void executeWindowsScript() throws IOException, InterruptedException {
        new ProcessBuilder(RESTART_WINDOWS).inheritIO().start().waitFor();
    }

    private static void executeLinuxScript() throws IOException, InterruptedException {
        new ProcessBuilder("./" + RESTART_LINUX).inheritIO().start().waitFor();
    }

    public static void main(String[] args) {
        try {
            System.out.println("Starte SyStatz-Neustart...");
            restartSyStatz();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
