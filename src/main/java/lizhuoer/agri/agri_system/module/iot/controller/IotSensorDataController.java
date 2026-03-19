package lizhuoer.agri.agri_system.module.iot.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.service.IIotSensorDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iot/data")
public class IotSensorDataController {

    private final IIotSensorDataService sensorDataService;

    public IotSensorDataController(IIotSensorDataService sensorDataService) {
        this.sensorDataService = sensorDataService;
    }

    @GetMapping("/list")
    public R<PageResult<IotSensorData>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String plotId,
            Long farmlandId,
            String sensorType) {
        Page<IotSensorData> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<IotSensorData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(plotId), IotSensorData::getPlotId, plotId)
                .eq(farmlandId != null, IotSensorData::getFarmlandId, farmlandId)
                .eq(StrUtil.isNotBlank(sensorType), IotSensorData::getSensorType, sensorType)
                .orderByDesc(IotSensorData::getCreateTime);
        return R.ok(PageResult.from(sensorDataService.page(page, wrapper)));
    }
}
