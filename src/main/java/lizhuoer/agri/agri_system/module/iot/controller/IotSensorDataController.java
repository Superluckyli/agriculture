package lizhuoer.agri.agri_system.module.iot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.service.IIotSensorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iot/data")
public class IotSensorDataController {

    @Autowired
    private IIotSensorDataService sensorDataService;

    @GetMapping("/list")
    public R<Page<IotSensorData>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String plotId,
            String sensorType) {
        Page<IotSensorData> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<IotSensorData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(plotId), IotSensorData::getPlotId, plotId)
                .eq(StrUtil.isNotBlank(sensorType), IotSensorData::getSensorType, sensorType)
                .orderByDesc(IotSensorData::getCreateTime);
        return R.ok(sensorDataService.page(page, wrapper));
    }
}
