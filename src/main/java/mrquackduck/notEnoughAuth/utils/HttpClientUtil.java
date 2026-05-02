package mrquackduck.notEnoughAuth.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public abstract class HttpClientUtil {
    public static String httpPostForm(String url, String body, String contentType) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Accept", "application/json");

        byte[] out = body.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(out.length);
        conn.connect();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(out);
        }

        int status = conn.getResponseCode();
        InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String response = readAll(is);
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " from " + url + " body: " + response);
        }
        return response;
    }

    public static String httpPostFormWithBasicAuth(String endpoint, String body, String basicAuth) throws IOException {
        java.net.URL url = new java.net.URL(endpoint);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Basic " + basicAuth);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String response;
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            response = sb.toString();
        }

        if (code < 200 || code >= 300) {
            throw new IOException("Telegram token endpoint failed: HTTP " + code + " body=" + response);
        }
        return response;
    }

    public static String httpGet(String url, String accept, String userAgent) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setRequestProperty("Accept", accept);
        conn.setRequestProperty("User-Agent", userAgent);

        int status = conn.getResponseCode();
        InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String response = readAll(is);
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " from " + url + " body: " + response);
        }
        return response;
    }

    public static String httpGet(String url, String accept, String userAgent, Map<String, String> headers)
            throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", accept)
                .header("User-Agent", userAgent);

        if (headers != null) {
            for (var kv : headers.entrySet()) {
                builder.header(kv.getKey(), kv.getValue());
            }
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP GET failed (" + status + "): " + response.body());
        }

        return response.body();
    }

    public static String httpGetWithAuth(String url, String accessToken, String userAgent) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        conn.setRequestProperty("User-Agent", userAgent);

        int status = conn.getResponseCode();
        InputStream is = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
        String response = readAll(is);
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " from " + url + " body: " + response);
        }
        return response;
    }

    private static String readAll(InputStream is) throws IOException {
        if (is == null) return "";
        byte[] buffer = new byte[4096];
        StringBuilder sb = new StringBuilder();
        int read;
        while ((read = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
