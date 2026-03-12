package lizhuoer.agri.agri_system.module.iot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.iot.domain.AgriTaskRule;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.mapper.AgriTaskRuleMapper;
import lizhuoer.agri.agri_system.module.iot.service.IAgriTaskRuleService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgriTaskRuleServiceImpl extends ServiceImpl<AgriTaskRuleMapper, AgriTaskRule>
        implements IAgriTaskRuleService {

    @Autowired
    private IAgriTaskService taskService;

    @Override
    public void checkAndTriggerTask(IotSensorData data) {
        // 1. 查询该传感器类型下，所有启用的规则
        List<AgriTaskRule> rules = this.list(new LambdaQueryWrapper<AgriTaskRule>()
                .eq(AgriTaskRule::getSensorType, data.getSensorType())
                .eq(AgriTaskRule::getIsEnable, 1));

        if (CollUtil.isEmpty(rules)) {
            return;
        }

        for (AgriTaskRule rule : rules) {
            boolean triggered = false;
            // 2. 判断是否超标
            if (rule.getMinVal() != null && data.getValue().compareTo(rule.getMinVal()) < 0) {
                triggered = true;
            }
            if (rule.getMaxVal() != null && data.getValue().compareTo(rule.getMaxVal()) > 0) {
                triggered = true;
            }

            // 3. 触发任务生成
            if (triggered) {
                createAlertTask(data, rule);
            }
        }
    }

    private void createAlertTask(IotSensorData data, AgriTaskRule rule) {
        // TODO: 实际生产中应有防抖机制（Avoid Duplicate Alert Task within 1 hour）
        // 这里简化：每次超标都生成一条，请在演示时注意数据清理

        AgriTask task = new AgriTask();
        // 任务名：[自动预警] 地块xxx 湿度过低
        task.setTaskName("[自动预警] 地块" + data.getPlotId() + " " + rule.getRuleName());
        task.setTaskType(rule.getAutoTaskType()); // e.g. "灌溉"
        task.setPriority(rule.getPriority());
        task.setPlanTime(LocalDateTime.now().plusHours(2)); // 要求2小时内处理
        task.setStatusV2("pending_accept");

        // 绑定批次ID (此处简化，暂时不查CropBatch，实际应根据PlotId反查当前正在种植的BatchId)
        // task.setBatchId(...);

        taskService.createAutoTask(task);
    }
}
