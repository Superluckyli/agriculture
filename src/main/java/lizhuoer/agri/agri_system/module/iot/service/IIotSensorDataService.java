package lizhuoer.agri.agri_system.module.iot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;

public interface IIotSensorDataService extends IService<IotSensorData> {
    /**
     * 保存数据并触发预警检查
     */
    void saveDataAndCheckAlert(IotSensorData data);
}
