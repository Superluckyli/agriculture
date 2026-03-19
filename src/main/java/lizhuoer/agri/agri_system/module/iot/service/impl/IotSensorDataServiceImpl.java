package lizhuoer.agri.agri_system.module.iot.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.farmland.domain.AgriFarmland;
import lizhuoer.agri.agri_system.module.crop.farmland.mapper.AgriFarmlandMapper;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.mapper.IotSensorDataMapper;
import lizhuoer.agri.agri_system.module.iot.service.IAgriTaskRuleService;
import lizhuoer.agri.agri_system.module.iot.service.IIotSensorDataService;
import lizhuoer.agri.agri_system.module.iot.sse.SseEmitterManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class IotSensorDataServiceImpl extends ServiceImpl<IotSensorDataMapper, IotSensorData>
        implements IIotSensorDataService {

    private final IAgriTaskRuleService ruleService;
    private final SseEmitterManager sseManager;
    private final AgriFarmlandMapper farmlandMapper;

    public IotSensorDataServiceImpl(IAgriTaskRuleService ruleService,
            SseEmitterManager sseManager,
            AgriFarmlandMapper farmlandMapper) {
        this.ruleService = ruleService;
        this.sseManager = sseManager;
        this.farmlandMapper = farmlandMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDataAndCheckAlert(IotSensorData data) {
        LocalDateTime now = LocalDateTime.now();
        if (data.getCreateTime() == null) {
            data.setCreateTime(now);
        }
        if (data.getCreatedAt() == null) {
            data.setCreatedAt(now);
        }
        if (StrUtil.isBlank(data.getSourceType())) {
            data.setSourceType("SIMULATED");
        }
        if (StrUtil.isBlank(data.getQualityStatus())) {
            data.setQualityStatus("VALID");
        }
        if (StrUtil.isBlank(data.getPlotId()) && data.getFarmlandId() != null) {
            data.setPlotId(resolvePlotId(data.getFarmlandId()));
        }

        this.save(data);
        sseManager.broadcast("iot-data", buildPayload(data));
        ruleService.checkAndTriggerTask(data);
    }

    private Map<String, Object> buildPayload(IotSensorData data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("dataId", data.getDataId());
        payload.put("deviceId", data.getDeviceId());
        payload.put("farmlandId", data.getFarmlandId());
        payload.put("plotId", data.getPlotId());
        payload.put("sensorType", data.getSensorType());
        payload.put("value", data.getSensorValue());
        payload.put("unit", data.getUnit());
        payload.put("sourceType", data.getSourceType());
        payload.put("qualityStatus", data.getQualityStatus());
        payload.put("createTime", data.getCreateTime());
        payload.put("createdAt", data.getCreatedAt());
        return payload;
    }

    private String resolvePlotId(Long farmlandId) {
        AgriFarmland farmland = farmlandMapper.selectById(farmlandId);
        if (farmland == null) {
            return String.valueOf(farmlandId);
        }
        if (StrUtil.isNotBlank(farmland.getCode())) {
            return farmland.getCode();
        }
        return String.valueOf(farmlandId);
    }
}
