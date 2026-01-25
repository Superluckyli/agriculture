package lizhuoer.agri.agri_system.module.iot.job;

import cn.hutool.core.util.RandomUtil;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.service.IIotSensorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 模拟传感器定时上报数据
 */
@Component
@EnableScheduling
public class SensorDataSimulator {

    @Autowired
    private IIotSensorDataService sensorDataService;

    // 模拟的地块列表
    private static final List<String> MOCK_PLOTS = Arrays.asList("Plot_A", "Plot_B", "Greenhouse_1");

    /**
     * 每 30 秒执行一次模拟 (演示频率快一些)
     */
    @Scheduled(fixedRate = 600000)
    public void generateMockData() {
        System.out.println(">>> [Simulator] 开始生成模拟传感器数据...");

        for (String plotId : MOCK_PLOTS) {
            // 1. 生成温度 (15~35度)
            double tempVal = RandomUtil.randomDouble(15.0, 35.0);
            createAndSave(plotId, "TEMP", tempVal, "℃");

            // 2. 生成湿度 (20%~90%)
            // 偶尔生成极低湿度以触发报警 (假设 20% 概率生成 < 30% 的干旱数据)
            double humVal;
            if (RandomUtil.randomInt(100) < 20) {
                humVal = RandomUtil.randomDouble(10.0, 29.0); // 触发报警
                System.out.println("    !!! 模拟生成 [异常] 湿度数据: " + humVal);
            } else {
                humVal = RandomUtil.randomDouble(30.0, 90.0); // 正常
            }
            createAndSave(plotId, "HUMIDITY", humVal, "%");
        }
    }

    private void createAndSave(String plotId, String type, double val, String unit) {
        IotSensorData data = new IotSensorData();
        data.setPlotId(plotId);
        data.setSensorType(type);
        data.setValue(BigDecimal.valueOf(val));
        data.setUnit(unit);
        data.setCreateTime(LocalDateTime.now());

        sensorDataService.saveDataAndCheckAlert(data);
    }
}
