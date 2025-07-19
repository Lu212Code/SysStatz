package lu212.sysStats.General;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import lu212.sysStats.SysStats_Web.SysStatsWebApplication;

public class AppReloader {

    private static ConfigurableApplicationContext context;
    private static String[] launchArgs;

    public static void setContext(ConfigurableApplicationContext ctx, String[] args) {
        context = ctx;
        launchArgs = args;
    }

    public static void restart() {
        if (context != null && launchArgs != null) {
            Thread thread = new Thread(() -> {
                context.close();
                SpringApplication.run(SysStatsWebApplication.class, launchArgs);
            });
            thread.setDaemon(false);
            thread.start();
        } else {
            System.err.println("Restart fehlgeschlagen: context oder args sind null.");
        }
    }
}
