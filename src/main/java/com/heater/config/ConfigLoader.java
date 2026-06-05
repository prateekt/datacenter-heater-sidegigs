package com.heater.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class ConfigLoader {

    private ConfigLoader() {}

    public static Map<String, Object> load(String path) throws IOException {
        Yaml yaml = new Yaml();
        if (path != null && !path.isBlank()) {
            try (InputStream in = Files.newInputStream(Path.of(path))) {
                return yaml.load(in);
            }
        }
        Path defaultPath = Path.of("config", "default.yaml");
        if (Files.exists(defaultPath)) {
            try (InputStream in = Files.newInputStream(defaultPath)) {
                return yaml.load(in);
            }
        }
        throw new IOException("Config not found: " + path);
    }

    public static double d(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : def;
    }

    public static Map<String, Object> map(Map<String, Object> root, String key) {
        Object v = root.get(key);
        return v instanceof Map ? (Map<String, Object>) v : Map.of();
    }

    public static List<String> stringList(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
