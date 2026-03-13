package lizhuoer.agri.agri_system.module.iot.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.batch.mapper.AgriCropBatchMapper;
import lizhuoer.agri.agri_system.module.iot.domain.AgriTaskRule;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.mapper.AgriTaskRuleMapper;
import lizhuoer.agri.agri_system.module.iot.service.IAgriTaskRuleService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatusV2;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgriTaskRuleServiceImpl extends ServiceImpl<AgriTaskRuleMapper, AgriTaskRule>
        implements IAgriTaskRuleService {

    private static final Logger log = LoggerFactory.getLogger(AgriTaskRuleServiceImpl.class);

    @Autowired
    private IAgriTaskService taskService;

    @Autowired
    private AgriCropBatchMapper batchMapper;

    @Override
    public void checkAndTriggerTask(IotSensorData data) {
        List<AgriTaskRule> rules = this.list(new LambdaQueryWrapper<AgriTaskRule>()
                .eq(AgriTaskRule::getSensorType, data.getSensorType())
                .eq(AgriTaskRule::getIsEnable, 1));

        if (CollUtil.isEmpty(rules)) {
            return;
        }

        for (AgriTaskRule rule : rules) {
            boolean triggered = false;
            if (rule.getMinVal() != null && data.getValue().compareTo(rule.getMinVal()) < 0) {
                triggered = true;
            }
            if (rule.getMaxVal() != null && data.getValue().compareTo(rule.getMaxVal()) > 0) {
                triggered = true;
            }
            if (triggered) {
                createAlertTask(data, rule);
            }
        }
    }

    private void createAlertTask(IotSensorData data, AgriTaskRule rule) {
        // plotId -> farmlandId 类型解析
        Long farmlandId;
        try {
            farmlandId = Long.parseLong(data.getPlotId());
        } catch (NumberFormatException e) {
            log.warn("IoT plotId 无法解析为 Long: {}", data.getPlotId());
            return;
        }

        // 反查活跃批次
        Long batchId = batchMapper.selectActiveBatchIdByFarmlandId(farmlandId);
        if (batchId == null) {
            log.warn("地块 {} 无活跃批次，跳过 IoT 任务创建", farmlandId);
            return;
        }

        // 1 小时防抖：同一 rule + 同一 farmland，1 小时内不重复创建
        long recentCount = taskService.count(new LambdaQueryWrapper<AgriTask>()
                .eq(AgriTask::getSourceRuleId, rule.getRuleId())
                .eq(AgriTask::getSourceFarmlandId, farmlandId)
                .ge(AgriTask::getCreateTime, LocalDateTime.now().minusHours(1)));
        if (recentCount > 0) {
            log.debug("IoT 防抖: rule={} farmland={} 1小时内已有任务，跳过",
                    rule.getRuleId(), farmlandId);
            return;
        }

        AgriTask task = new AgriTask();
        task.setTaskName("[自动预警] 地块" + data.getPlotId() + " " + rule.getRuleName());
        task.setTaskType(rule.getAutoTaskType());
        task.setPriority(rule.getPriority());
        task.setPlanTime(LocalDateTime.now().plusHours(2));
        task.setBatchId(batchId);
        task.setTaskSource("rule");
        task.setSourceRuleId(rule.getRuleId());
        task.setSourceFarmlandId(farmlandId);

        taskService.createAutoTask(task);
    }
}
