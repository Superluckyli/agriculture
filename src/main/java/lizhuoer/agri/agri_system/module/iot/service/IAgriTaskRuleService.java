package lizhuoer.agri.agri_system.module.iot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.iot.domain.AgriTaskRule;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;

public interface IAgriTaskRuleService extends IService<AgriTaskRule> {
    /**
     * 根据传感器数据检查是否触发规则
     */
    void checkAndTriggerTask(IotSensorData data);
}
