package lizhuoer.agri.agri_system.module.iot.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.crop.batch.mapper.AgriCropBatchMapper;
import lizhuoer.agri.agri_system.module.crop.farmland.domain.AgriFarmland;
import lizhuoer.agri.agri_system.module.crop.farmland.mapper.AgriFarmlandMapper;
import lizhuoer.agri.agri_system.module.iot.domain.AgriTaskRule;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import lizhuoer.agri.agri_system.module.iot.domain.IotTaskDispatchRecord;
import lizhuoer.agri.agri_system.module.iot.domain.IotWarningEvent;
import lizhuoer.agri.agri_system.module.iot.mapper.AgriTaskRuleMapper;
import lizhuoer.agri.agri_system.module.iot.mapper.IotTaskDispatchRecordMapper;
import lizhuoer.agri.agri_system.module.iot.mapper.IotWarningEventMapper;
import lizhuoer.agri.agri_system.module.iot.service.IAgriTaskRuleService;
import lizhuoer.agri.agri_system.module.iot.support.IotSensorCatalog;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgriTaskRuleServiceImpl extends ServiceImpl<AgriTaskRuleMapper, AgriTaskRule>
        implements IAgriTaskRuleService {

    private static final Logger log = LoggerFactory.getLogger(AgriTaskRuleServiceImpl.class);
    private static final String MODE_MANUAL = "MANUAL";
    private static final String MODE_AUTO = "AUTO";
    private static final String MODE_AUTO_AI = "AUTO_AI";

    private final IAgriTaskService taskService;
    private final AgriCropBatchMapper batchMapper;
    private final AgriFarmlandMapper farmlandMapper;
    private final IotWarningEventMapper warningEventMapper;
    private final IotTaskDispatchRecordMapper dispatchRecordMapper;

    public AgriTaskRuleServiceImpl(IAgriTaskService taskService,
            AgriCropBatchMapper batchMapper,
            AgriFarmlandMapper farmlandMapper,
            IotWarningEventMapper warningEventMapper,
            IotTaskDispatchRecordMapper dispatchRecordMapper) {
        this.taskService = taskService;
        this.batchMapper = batchMapper;
        this.farmlandMapper = farmlandMapper;
        this.warningEventMapper = warningEventMapper;
        this.dispatchRecordMapper = dispatchRecordMapper;
    }

    @Override
    public boolean save(AgriTaskRule entity) {
        normalizeRule(entity);
        return super.save(entity);
    }

    @Override
    public boolean updateById(AgriTaskRule entity) {
        normalizeRule(entity);
        return super.updateById(entity);
    }

    // 检查预警规则
    @Override
    public void checkAndTriggerTask(IotSensorData data) {
        if (!isDataUsable(data) || data.getFarmlandId() == null) {
            return;
        }
        // 获取所有启用的预警规则
        List<AgriTaskRule> rules = this.list(new LambdaQueryWrapper<AgriTaskRule>()
                .eq(AgriTaskRule::getSensorType, normalizeSensorType(data.getSensorType()))
                .eq(AgriTaskRule::getIsEnable, 1));
        if (CollUtil.isEmpty(rules)) {
            return;
        }
        // 遍历所有启用的预警规则
        for (AgriTaskRule rule : rules) {
            normalizeRule(rule);
            if (isTriggered(rule, data.getSensorValue())) {
                // 创建预警事件并派单
                createWarningEventAndDispatch(data, rule);
            }
        }
    }

    // 检查数据是否可用
    private boolean isDataUsable(IotSensorData data) {
        if (data == null || data.getSensorValue() == null) {
            return false;
        }
        return !"INVALID".equalsIgnoreCase(StrUtil.blankToDefault(data.getQualityStatus(), "VALID"));
    }

    // 检查是否触发
    private boolean isTriggered(AgriTaskRule rule, BigDecimal sensorValue) {
        String triggerCondition = StrUtil.blankToDefault(rule.getTriggerCondition(), "OUTSIDE_RANGE");
        return switch (triggerCondition) {
            case "LT" -> rule.getMinVal() != null && sensorValue.compareTo(rule.getMinVal()) < 0;
            case "GT" -> rule.getMaxVal() != null && sensorValue.compareTo(rule.getMaxVal()) > 0;
            default -> (rule.getMinVal() != null && sensorValue.compareTo(rule.getMinVal()) < 0)
                    || (rule.getMaxVal() != null && sensorValue.compareTo(rule.getMaxVal()) > 0);
        };
    }

    // 创建预警事件并派单
    private void createWarningEventAndDispatch(IotSensorData data, AgriTaskRule rule) {
        LocalDateTime now = LocalDateTime.now();
        Long batchId = batchMapper.selectActiveBatchIdByFarmlandId(data.getFarmlandId());
        String mode = normalizeCreateMode(rule.getCreateMode());

        IotWarningEvent event = new IotWarningEvent();
        event.setRuleId(rule.getRuleId());
        event.setSensorDataId(data.getDataId());
        event.setDeviceId(data.getDeviceId());
        event.setFarmlandId(data.getFarmlandId());
        event.setBatchId(batchId);
        event.setSensorType(normalizeSensorType(data.getSensorType()));
        event.setTriggerValue(data.getSensorValue());
        event.setTriggeredAt(data.getCreateTime() != null ? data.getCreateTime() : now);
        event.setHandleStatus("PENDING");
        event.setDispatchMode(mode);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        warningEventMapper.insert(event);

        if (MODE_MANUAL.equals(mode)) {
            return;
        }

        if (batchId == null) {
            markDispatchFailed(event, mode, "当前地块无活跃批次，无法自动创建任务");
            return;
        }
        // 查找可复用的任务
        AgriTask reusableTask = findReusableTask(rule, data.getFarmlandId(), batchId, event.getTriggeredAt());
        if (reusableTask != null) {
            event.setTaskId(reusableTask.getTaskId());
            event.setHandleStatus("TASK_LINKED");
            event.setUpdatedAt(LocalDateTime.now());
            warningEventMapper.updateById(event);
            saveDispatchRecord(event, mode, "LINKED_EXISTING", reusableTask.getTaskId(), null, null);
            return;
        }

        // 创建任务
        try {
            AgriTask task = buildTask(rule, data, batchId);
            taskService.createAutoTask(task);
            event.setTaskId(task.getTaskId());
            event.setHandleStatus("TASK_CREATED");
            event.setUpdatedAt(LocalDateTime.now());
            warningEventMapper.updateById(event);
            saveDispatchRecord(event, mode, "SUCCESS", task.getTaskId(), null, null);
        } catch (Exception ex) {
            log.warn("Failed to create IoT task for rule={} farmland={} batch={}",
                    rule.getRuleId(), data.getFarmlandId(), batchId, ex);
            markDispatchFailed(event, mode, ex.getMessage());
        }
    }

    // 构建任务
    private AgriTask buildTask(AgriTaskRule rule, IotSensorData data, Long batchId) {
        LocalDateTime now = LocalDateTime.now();
        AgriTask task = new AgriTask();
        task.setTaskName(buildTaskName(rule, data.getFarmlandId()));
        task.setTaskType(StrUtil.blankToDefault(rule.getAutoTaskType(), "IoT预警处理"));
        task.setPriority(rule.getPriority() != null ? rule.getPriority() : 2);
        task.setPlanTime(now.plusHours(2));
        task.setBatchId(batchId);
        task.setTaskSource("iot_warning");
        task.setSourceRuleId(rule.getRuleId());
        task.setSourceFarmlandId(data.getFarmlandId());
        task.setPrecautionNote("传感器[" + data.getSensorType() + "] 当前值=" + data.getSensorValue());
        if (MODE_AUTO_AI.equals(normalizeCreateMode(rule.getCreateMode()))) {
            task.setSuggestAction("预留 AI 辅助生成任务描述，当前使用系统默认任务内容。");
        }
        return task;
    }

    // 构建任务名称
    private String buildTaskName(AgriTaskRule rule, Long farmlandId) {
        String farmlandLabel = "地块" + farmlandId;
        AgriFarmland farmland = farmlandMapper.selectById(farmlandId);
        if (farmland != null) {
            if (StrUtil.isNotBlank(farmland.getCode())) {
                farmlandLabel = farmland.getCode();
            } else if (StrUtil.isNotBlank(farmland.getName())) {
                farmlandLabel = farmland.getName();
            }
        }
        return "[IoT预警] " + farmlandLabel + " " + rule.getRuleName();
    }

    // 查找可重用的任务
    private AgriTask findReusableTask(AgriTaskRule rule, Long farmlandId, Long batchId, LocalDateTime triggeredAt) {
        int cooldown = rule.getCooldownMinutes() != null && rule.getCooldownMinutes() > 0
                ? rule.getCooldownMinutes()
                : 60;
        LocalDateTime threshold = triggeredAt.minusMinutes(cooldown);
        return taskService.getOne(new LambdaQueryWrapper<AgriTask>()
                .eq(AgriTask::getSourceRuleId, rule.getRuleId())
                .eq(AgriTask::getSourceFarmlandId, farmlandId)
                .eq(AgriTask::getBatchId, batchId)
                .ge(AgriTask::getCreateTime, threshold)
                .orderByDesc(AgriTask::getCreateTime)
                .last("LIMIT 1"), false);
    }

    // 标记派单失败
    private void markDispatchFailed(IotWarningEvent event, String mode, String reason) {
        event.setHandleStatus("FAILED");
        event.setDispatchMode(mode);
        event.setFailureReason(StrUtil.blankToDefault(reason, "自动创建任务失败"));
        event.setUpdatedAt(LocalDateTime.now());
        warningEventMapper.updateById(event);
        saveDispatchRecord(event, mode, "FAILED", null, null, event.getFailureReason());
    }

    // 保存派单记录
    private void saveDispatchRecord(IotWarningEvent event,
            String mode,
            String status,
            Long taskId,
            String aiSummary,
            String errorMessage) {
        IotTaskDispatchRecord record = new IotTaskDispatchRecord();
        record.setEventId(event.getEventId());
        record.setRuleId(event.getRuleId());
        record.setTaskId(taskId);
        record.setDispatchMode(mode);
        record.setDispatchStatus(status);
        record.setAiSummary(aiSummary);
        record.setErrorMessage(errorMessage);
        record.setCreatedAt(LocalDateTime.now());
        dispatchRecordMapper.insert(record);
    }

    // 规范化规则
    private void normalizeRule(AgriTaskRule rule) {
        if (rule == null) {
            return;
        }
        rule.setSensorType(normalizeSensorType(rule.getSensorType()));
        if (StrUtil.isBlank(rule.getTriggerCondition())) {
            rule.setTriggerCondition(inferTriggerCondition(rule));
        }
        rule.setCreateMode(normalizeCreateMode(rule.getCreateMode()));
        if (rule.getPriority() == null) {
            rule.setPriority(2);
        }
        if (rule.getCooldownMinutes() == null || rule.getCooldownMinutes() <= 0) {
            rule.setCooldownMinutes(60);
        }
        if (rule.getIsEnable() == null) {
            rule.setIsEnable(1);
        }
    }

    private String inferTriggerCondition(AgriTaskRule rule) {
        if (rule.getMinVal() != null && rule.getMaxVal() == null) {
            return "LT";
        }
        if (rule.getMinVal() == null && rule.getMaxVal() != null) {
            return "GT";
        }
        return "OUTSIDE_RANGE";
    }

    private String normalizeCreateMode(String createMode) {
        String raw = StrUtil.blankToDefault(createMode, MODE_AUTO).trim().toUpperCase();
        if (MODE_MANUAL.equals(raw) || MODE_AUTO_AI.equals(raw)) {
            return raw;
        }
        return MODE_AUTO;
    }

    private String normalizeSensorType(String sensorType) {
        return IotSensorCatalog.normalize(sensorType);
    }
}
