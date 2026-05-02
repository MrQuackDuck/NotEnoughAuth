package mrquackduck.notEnoughAuth.services.helpers;

import com.sun.net.httpserver.HttpExchange;
import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.configuration.Configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpResponseHelper {
    private final NotEnoughAuth plugin;
    private final Configuration config;

    public HttpResponseHelper(NotEnoughAuth plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
    }

    public void sendSuccess(HttpExchange exchange, int statusCode, String title, String text) throws IOException {
        var html = renderTemplate(loadResourceText(config.successWebPagePath()),
                Map.of("title", title,
                        "message", text));

        sendHtml(exchange, statusCode, html);
    }

    public void sendError(HttpExchange exchange, int statusCode, String title, String text) throws IOException {
        var html = renderTemplate(loadResourceText(config.errorWebPagePath()),
                Map.of("title", title,
                        "message", text));

        sendHtml(exchange, statusCode, html);
    }

    public void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        sendText(exchange, statusCode, html, "text/html; charset=utf-8");
    }

    public String loadResourceText(String resourcePath) throws IOException {
        try (var is = plugin.getResource(resourcePath)) {
            if (is == null) throw new IOException("Missing resource: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String renderTemplate(String template, Map<String, String> values) {
        String out = template;
        for (var entry : values.entrySet()) {
            out = out.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return out;
    }

    public void sendText(HttpExchange exchange, int statusCode, String text, String contentType) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
