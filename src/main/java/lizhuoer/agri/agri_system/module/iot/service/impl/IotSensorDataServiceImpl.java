package lizhuoer.agri.agri_system.module.iot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.mapper.IotSensorDataMapper;
import lizhuoer.agri.agri_system.module.iot.service.IAgriTaskRuleService;
import lizhuoer.agri.agri_system.module.iot.service.IIotSensorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IotSensorDataServiceImpl extends ServiceImpl<IotSensorDataMapper, IotSensorData>
        implements IIotSensorDataService {

    @Autowired
    private IAgriTaskRuleService ruleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDataAndCheckAlert(IotSensorData data) {
        // 1. 入库
        this.save(data);

        // 2. 检查警报 (异步或同步均可，演示用同步)
        ruleService.checkAndTriggerTask(data);
    }
}
