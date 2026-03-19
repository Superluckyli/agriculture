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

    @Scheduled(fixedRate = 60000)
    public void generateMockData() {
        LocalDateTime now = LocalDateTime.now();
        List<IotDevice> devices = deviceMapper.selectList(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getSourceType, "SIMULATED")
                .ne(IotDevice::getDeviceStatus, "DISABLED"));
        if (devices.isEmpty()) {
            return;
        }

        for (IotDevice device : devices) {
            IotDeviceBinding binding = bindingMapper.selectOne(new LambdaQueryWrapper<IotDeviceBinding>()
                    .eq(IotDeviceBinding::getDeviceId, device.getDeviceId())
                    .eq(IotDeviceBinding::getIsActive, 1)
                    .last("LIMIT 1"));
            if (binding == null) {
                continue;
            }

            AgriFarmland farmland = farmlandMapper.selectById(binding.getFarmlandId());
            String plotId = farmland != null && StrUtil.isNotBlank(farmland.getCode())
                    ? farmland.getCode()
                    : String.valueOf(binding.getFarmlandId());

            List<IotSimulationProfile> profiles = profileMapper.selectList(new LambdaQueryWrapper<IotSimulationProfile>()
                    .eq(IotSimulationProfile::getDeviceId, device.getDeviceId())
                    .eq(IotSimulationProfile::getIsEnabled, 1));
            for (IotSimulationProfile profile : profiles) {
                if (!IotSensorCatalog.isSupported(profile.getSensorType())) {
                    continue;
                }
                if (!shouldGenerate(profile, now)) {
                    continue;
                }

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
                sensorDataService.saveDataAndCheckAlert(data);
            }

            device.setLastReportedAt(now);
            deviceMapper.updateById(device);
        }
    }

    private boolean shouldGenerate(IotSimulationProfile profile, LocalDateTime now) {
        int interval = profile.getIntervalSeconds() != null && profile.getIntervalSeconds() > 0
                ? profile.getIntervalSeconds()
                : 600;
        LocalDateTime latestReportedAt = sensorDataMapper.selectLatestReportedAt(profile.getDeviceId(), profile.getSensorType());
        return latestReportedAt == null
                || latestReportedAt.plusSeconds(interval).isBefore(now)
                || latestReportedAt.plusSeconds(interval).isEqual(now);
    }

    private BigDecimal generateValue(IotSimulationProfile profile) {
        double base = profile.getBaseValue() != null ? profile.getBaseValue().doubleValue() : 0D;
        double range = profile.getFluctuationRange() != null ? profile.getFluctuationRange().doubleValue() : 0D;
        double probability = profile.getWarningProbability() != null ? profile.getWarningProbability().doubleValue() : 0D;
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
