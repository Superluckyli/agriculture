package lizhuoer.agri.agri_system.module.iot.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class IotSensorCatalogTest {

    @Test
    void supportedSensorsShouldExcludeEcAndIncludeTheFiveMonitorTypes() {
        assertEquals(List.of("temperature", "humidity", "light", "soil_moisture", "ph"),
                IotSensorCatalog.supportedSensorTypes());
    }

    @Test
    void normalizeShouldKeepSupportedAliasesAndTreatEcAsUnsupported() {
        assertEquals("temperature", IotSensorCatalog.normalize("temp"));
        assertEquals("soil_moisture", IotSensorCatalog.normalize("soilmoisture"));
        assertFalse(IotSensorCatalog.isSupported("ec"));
    }

    @Test
    void resolveUnitShouldCoverTheSupportedDisplaySensors() {
        assertEquals("lx", IotSensorCatalog.resolveUnit("light"));
        assertEquals("%", IotSensorCatalog.resolveUnit("soil_moisture"));
        assertEquals("", IotSensorCatalog.resolveUnit("ph"));
    }
}
