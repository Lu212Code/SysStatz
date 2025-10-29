package lu212.sysstatz.SysStats_Web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ConsoleController consoleController;

    public WebSocketConfig(ConsoleController consoleController) {
        this.consoleController = consoleController;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(consoleController.new LogWebSocketHandler(), "/ws/logs").setAllowedOrigins("*");
    }
}
