package lizhuoer.agri.agri_system.module.iot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.mapper.IotSensorDataMapper;
import lizhuoer.agri.agri_system.module.iot.service.IAgriTaskRuleService;
import lizhuoer.agri.agri_system.module.iot.service.IIotSensorDataService;
import lizhuoer.agri.agri_system.module.iot.sse.SseEmitterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class IotSensorDataServiceImpl extends ServiceImpl<IotSensorDataMapper, IotSensorData>
        implements IIotSensorDataService {

    @Autowired
    private IAgriTaskRuleService ruleService;

    @Autowired
    private SseEmitterManager sseManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDataAndCheckAlert(IotSensorData data) {
        // 1. 入库
        this.save(data);

        // 2. SSE 广播给前端订阅者
        Map<String, Object> payload = new HashMap<>();
        payload.put("dataId", data.getDataId());
        payload.put("plotId", data.getPlotId());
        payload.put("sensorType", data.getSensorType());
        payload.put("value", data.getValue());
        payload.put("unit", data.getUnit());
        payload.put("createTime", data.getCreateTime());
        sseManager.broadcast("iot-data", payload);

        // 3. 检查警报
        ruleService.checkAndTriggerTask(data);
    }
}
