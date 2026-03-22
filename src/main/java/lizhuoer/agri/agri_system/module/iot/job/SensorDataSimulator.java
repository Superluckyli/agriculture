package lizhuoer.agri.agri_system.module.iot.job;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.module.crop.farmland.domain.AgriFarmland;
import lizhuoer.agri.agri_system.module.crop.farmland.mapper.AgriFarmlandMapper;
import lizhuoer.agri.agri_system.module.iot.domain.IotDevice;
import lizhuoer.agri.agri_system.module.iot.domain.IotDeviceBinding;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.domain.IotSimulationProfile;
import lizhuoer.agri.agri_system.module.iot.mapper.IotDeviceBindingMapper;
import lizhuoer.agri.agri_system.module.iot.mapper.IotDeviceMapper;
import lizhuoer.agri.agri_system.module.iot.mapper.IotSensorDataMapper;
import lizhuoer.agri.agri_system.module.iot.mapper.IotSimulationProfileMapper;
import lizhuoer.agri.agri_system.module.iot.service.IIotSensorDataService;
import lizhuoer.agri.agri_system.module.iot.support.IotSensorCatalog;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * IoT 传感器数据模拟调度任务。
 * 该组件在无真实硬件环境下运行。定期扫描模拟设备配置，基于基准值与随机波动生成模拟监测数据，
 * 并存入数据库，同时负责触发后续的异常检测和待分配任务派单逻辑。
 */
@Component
@EnableScheduling
public class SensorDataSimulator {

    private final IIotSensorDataService sensorDataService;
    private final IotDeviceMapper deviceMapper;
    private final IotDeviceBindingMapper bindingMapper;
    private final IotSensorDataMapper sensorDataMapper;
    private final IotSimulationProfileMapper profileMapper;
    private final AgriFarmlandMapper farmlandMapper;

    public SensorDataSimulator(IIotSensorDataService sensorDataService,
            IotDeviceMapper deviceMapper,
            IotDeviceBindingMapper bindingMapper,
            IotSensorDataMapper sensorDataMapper,
            IotSimulationProfileMapper profileMapper,
            AgriFarmlandMapper farmlandMapper) {
        this.sensorDataService = sensorDataService;
        this.deviceMapper = deviceMapper;
        this.bindingMapper = bindingMapper;
        this.sensorDataMapper = sensorDataMapper;
        this.profileMapper = profileMapper;
        this.farmlandMapper = farmlandMapper;
    }

    /**
     * 定时生成模拟数据的入口方法。
     * 每分钟（60000 毫秒）执行一次，核心流转主要涉及：
     * 1. 获取所有"SIMULATED"（模拟接入）类型的激活设备
     * 2. 查询设备所绑定的地块Farmland信息
     * 3. 根据设备的模拟数据生成配置模板，按配置周期产生随机波动的数据点
     * 4. 驱动预警检查
     */
    @Scheduled(fixedRate = 60000)
    public void generateMockData() {
        LocalDateTime now = LocalDateTime.now();
        List<IotDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getSourceType, "SIMULATED")
                .ne(IotDevice::getDeviceStatus, "DISABLED"));
        if (devices.isEmpty()) {
            return;
        }
        // 遍历所有启用的模拟设备
        for (IotDevice device : devices) {
            IotDeviceBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<IotDeviceBinding>()
                    .eq(IotDeviceBinding::getDeviceId, device.getDeviceId())
                    .eq(IotDeviceBinding::getIsActive, 1)
                    .last("LIMIT 1"));
            if (binding == null) {
                continue;
            }
            // 获取地块信息
            AgriFarmland farmland = farmlandMapper.selectById(binding.getFarmlandId());
            String plotId = farmland != null && StrUtil.isNotBlank(farmland.getCode())
                    ? farmland.getCode()
                    : String.valueOf(binding.getFarmlandId());
            // 获取设备的模拟数据生成配置模板
            List<IotSimulationProfile> profiles = profileMapper
                    .selectList(new LambdaQueryWrapper<IotSimulationProfile>()
                            .eq(IotSimulationProfile::getDeviceId, device.getDeviceId())
                            .eq(IotSimulationProfile::getIsEnabled, 1));
            // 遍历所有启用的模拟数据生成配置模板
            for (IotSimulationProfile profile : profiles) {
                if (!IotSensorCatalog.isSupported(profile.getSensorType())) {
                    continue;
                }
                if (!shouldGenerate(profile, now)) {
                    continue;
                }
                // 创建传感器数据
                IotSensorData data = new IotSensorData();
                data.setDeviceId(device.getDeviceId());
                data.setFarmlandId(binding.getFarmlandId());
                data.setPlotId(plotId);
                data.setSensorType(profile.getSensorType());
                data.setSensorValue(generateValue(profile));
                data.setUnit(IotSensorCatalog.resolveUnit(profile.getSensorType()));
                data.setSourceType("SIMULATED");
                data.setQualityStatus("VALID");
                data.setCreateTime(now);
                data.setCreatedAt(now);
                // 保存传感器数据并检查预警规则
                sensorDataService.saveDataAndCheckAlert(data);
            }

            // 更新设备最后上报时间
            device.setLastReportedAt(now);
            // 更新设备状态
            deviceMapper.updateById(device);
        }
    }

    /**
     * 判断当前是否已经达到该设备配置生成下一条数据的时间周期。
     * 
     * @param profile 设备的独立模拟数值配置
     * @param now     常规时间（即当前时间）
     * @return 距离上次生成时间若已超过了配置的间隔时长(intervalSeconds)，则返回 true；否则 false
     */
    private boolean shouldGenerate(IotSimulationProfile profile, LocalDateTime now) {
        int interval = profile.getIntervalSeconds() != null && profile.getIntervalSeconds() > 0
                ? profile.getIntervalSeconds()
                : 600;
        LocalDateTime latestReportedAt = sensorDataMapper.selectLatestReportedAt(profile.getDeviceId(),
                profile.getSensorType());
        return latestReportedAt == null
                || latestReportedAt.plusSeconds(interval).isBefore(now)
                || latestReportedAt.plusSeconds(interval).isEqual(now);
    }

    /**
     * 根据设备配置的基准值、波动幅度以及异常概率，生成具备指定偏差特性的传感器随机读数。
     * 
     * @param profile 数据模拟配置（含基准值、波动范围、触发预警概率等设定）
     * @return 返回保留两位小数精度的随机 BigDecimal 数值
     */
    private BigDecimal generateValue(IotSimulationProfile profile) {
        double base = profile.getBaseValue() != null ? profile.getBaseValue().doubleValue() : 0D;
        double range = profile.getFluctuationRange() != null ? profile.getFluctuationRange().doubleValue() : 0D;
        double probability = profile.getWarningProbability() != null ? profile.getWarningProbability().doubleValue()
                : 0D;
        double target = profile.getWarningValue() != null ? profile.getWarningValue().doubleValue() : base;

        double value;
        if (RandomUtil.randomDouble(0D, 1D) < probability) {
            double offset = Math.max(0.3D, range * 0.25D);
            value = RandomUtil.randomDouble(target - offset, target + offset);
        } else {
            value = RandomUtil.randomDouble(base - range, base + range);
        }

        if (value < 0D) {
            value = 0D;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
