package lu212.sysStats.SysStats_Web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lu212.sysStats.StatsServer.*;

@Controller
public class CommandController {

    @PostMapping("/command")
    public String handleCommand(@RequestParam String command,
                                @RequestParam String serverName) {
        System.out.println("Befehl von Server '" + serverName + "' erhalten: " + command);

        triggerAction(serverName, command);

        return "redirect:/server/" + URLEncoder.encode(serverName, StandardCharsets.UTF_8);
    }

    private void triggerAction(String serverName, String command) {
    	System.out.println("Befehl für " + serverName + " wird ausgeführt: " + command);
    	Server.sendCMD(serverName, command);
    }
}
