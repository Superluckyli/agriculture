package lizhuoer.agri.agri_system.module.task.material.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatusV2;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import lizhuoer.agri.agri_system.module.task.material.domain.AgriTaskMaterial;
import lizhuoer.agri.agri_system.module.task.material.mapper.AgriTaskMaterialMapper;
import lizhuoer.agri.agri_system.module.task.material.service.IAgriTaskMaterialService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AgriTaskMaterialServiceImpl extends ServiceImpl<AgriTaskMaterialMapper, AgriTaskMaterial> implements IAgriTaskMaterialService {

    private static final Set<String> EDITABLE_STATUSES = Set.of(
            TaskStatusV2.PENDING_ACCEPT,
            TaskStatusV2.IN_PROGRESS
    );

    private final IAgriTaskService taskService;

    public AgriTaskMaterialServiceImpl(@Lazy IAgriTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<List<AgriTaskMaterial>> addMaterials(Long taskId, List<AgriTaskMaterial> items) {
        if (taskId == null || items == null || items.isEmpty()) {
            throw new RuntimeException("taskId 和耗材列表不能为空");
        }
        assertEditable(taskId);

        LocalDateTime now = LocalDateTime.now();
        for (AgriTaskMaterial item : items) {
            item.setId(null);
            item.setTaskId(taskId);
            item.setCreatedAt(now);
        }
        this.saveBatch(items);
        return R.ok(baseMapper.listWithNameByTaskId(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<AgriTaskMaterial> updateActualQty(Long id, BigDecimal actualQty, String deviationReason) {
        if (id == null || actualQty == null) {
            throw new RuntimeException("id 和 actualQty 不能为空");
        }
        AgriTaskMaterial record = this.getById(id);
        if (record == null) {
            throw new RuntimeException("耗材记录不存在: " + id);
        }
        assertEditable(record.getTaskId());

        record.setActualQty(actualQty);
        record.setDeviationReason(deviationReason);
        this.updateById(record);
        return R.ok(record);
    }

    @Override
    public R<List<AgriTaskMaterial>> listByTaskId(Long taskId) {
        if (taskId == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        return R.ok(baseMapper.listWithNameByTaskId(taskId));
    }

    private void assertEditable(Long taskId) {
        AgriTask task = taskService.getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        if (!EDITABLE_STATUSES.contains(task.getStatusV2())) {
            throw new RuntimeException("当前任务状态不允许编辑耗材: " + task.getStatusV2());
        }
    }
}
