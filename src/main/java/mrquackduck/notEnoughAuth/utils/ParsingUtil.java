package mrquackduck.notEnoughAuth.utils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public abstract class ParsingUtil {
    /**
     * JSON string field extractor: looks for `"fieldName":"value"` and returns value.
     */
    public static String extractJsonPrimitiveField(String json, String fieldName) {
        if (json == null || fieldName == null) return null;

        String needle = "\"" + fieldName + "\"";
        int idx = json.indexOf(needle);
        if (idx == -1) return null;

        int colon = json.indexOf(':', idx + needle.length());
        if (colon == -1) return null;

        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

        if (i >= json.length()) return null;

        // string
        if (json.charAt(i) == '"') {
            int start = i + 1;
            int end = json.indexOf('"', start);
            if (end == -1) return null;
            return json.substring(start, end);
        }

        // number / boolean / null -> read until delimiter
        int start = i;
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            end++;
        }

        String raw = json.substring(start, end);
        if (raw.equals("null")) return null;
        return raw;
    }

    /**
     * Extracts field from url-encoded form: key1=val1&key2=val2...
     */
    public static String extractUrlEncodedField(String form, String field) {
        if (form == null) return null;
        String[] parts = form.split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String key = part.substring(0, idx);
            String val = part.substring(idx + 1);
            if (key.equals(field)) {
                return URLDecoder.decode(val, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Parses URI into a string-string map
     */
    public static Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String q = uri.getRawQuery();
        if (q == null || q.isEmpty()) return map;

        String[] pairs = q.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) continue;
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            map.put(key, val);
        }
        return map;
    }

    /**
     * Encodes a string into URL (e.g., converts spaces into '%20', etc.)
     */
    public static String urlEncode(String url) {
        return java.net.URLEncoder.encode(url, StandardCharsets.UTF_8);
    }
}
