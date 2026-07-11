package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

/**
 * Utilitario JSON centralizado (Gson) para los servlets.
 * Serializa {@link LocalDateTime} como texto ISO-8601 para el frontend.
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, tipo, ctx) ->
                            src == null ? JsonNull.INSTANCE
                                    : new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();

    private JsonUtil() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static String toJson(Object objeto) {
        return GSON.toJson(objeto);
    }

    public static JsonObject parse(String cuerpo) {
        return JsonParser.parseString(cuerpo).getAsJsonObject();
    }
}
