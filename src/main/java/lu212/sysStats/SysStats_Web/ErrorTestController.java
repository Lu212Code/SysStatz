package lu212.sysStats.SysStats_Web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorTestController {

    @GetMapping("/trigger-error")
    public String triggerError() {
        int a = 1 / 0;  // löst ArithmeticException (Division durch Null) aus
        return "This won't be reached";
    }
}
