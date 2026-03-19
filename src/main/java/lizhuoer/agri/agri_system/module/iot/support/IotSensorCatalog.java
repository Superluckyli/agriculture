package lizhuoer.agri.agri_system.module.iot.support;

import java.util.List;
import java.util.Set;

public final class IotSensorCatalog {
    private static final List<String> SUPPORTED_SENSOR_TYPES = List.of(
            "temperature",
            "humidity",
            "light",
            "soil_moisture",
            "ph"
    );

    private static final Set<String> SUPPORTED_SENSOR_TYPE_SET = Set.copyOf(SUPPORTED_SENSOR_TYPES);

    private IotSensorCatalog() {
    }

    public static List<String> supportedSensorTypes() {
        return SUPPORTED_SENSOR_TYPES;
    }

    public static String normalize(String sensorType) {
        String raw = sensorType == null ? "" : sensorType.trim().toLowerCase();
        return switch (raw) {
            case "temp", "temperature" -> "temperature";
            case "humidity", "air_humidity" -> "humidity";
            case "light", "illumination" -> "light";
            case "soil_moisture", "soilmoisture", "moisture" -> "soil_moisture";
            case "ph" -> "ph";
            default -> raw;
        };
    }

    public static boolean isSupported(String sensorType) {
        return SUPPORTED_SENSOR_TYPE_SET.contains(normalize(sensorType));
    }

    public static String resolveUnit(String sensorType) {
        return switch (normalize(sensorType)) {
            case "temperature" -> "℃";
            case "humidity", "soil_moisture" -> "%";
            case "light" -> "lx";
            default -> "";
        };
    }
}
