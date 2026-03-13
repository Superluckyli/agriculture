package lizhuoer.agri.agri_system.module.task.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper;
import lizhuoer.agri.agri_system.module.material.stocklog.domain.MaterialStockLog;
import lizhuoer.agri.agri_system.module.material.stocklog.service.IMaterialStockLogService;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderItemService;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderService;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAcceptDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskAssignDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskRejectDTO;
import lizhuoer.agri.agri_system.module.task.domain.dto.TaskUpdateDTO;
import lizhuoer.agri.agri_system.module.task.domain.enums.TaskStatusV2;
import lizhuoer.agri.agri_system.module.task.log.domain.AgriTaskLog;
import lizhuoer.agri.agri_system.module.task.log.service.IAgriTaskLogService;
import lizhuoer.agri.agri_system.module.task.mapper.AgriTaskMapper;
import lizhuoer.agri.agri_system.module.task.material.domain.AgriTaskMaterial;
import lizhuoer.agri.agri_system.module.task.material.service.IAgriTaskMaterialService;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgriTaskServiceImpl extends ServiceImpl<AgriTaskMapper, AgriTask> implements IAgriTaskService {
    private static final String ROLE_WORKER = "WORKER";
    private static final int MAX_RETRY = 3;

    private final ISysUserService userService;
    private final IAgriTaskMaterialService taskMaterialService;
    private final IAgriTaskLogService taskLogService;
    private final MaterialInfoMapper materialInfoMapper;
    private final IMaterialStockLogService stockLogService;
    private final IPurchaseOrderService purchaseOrderService;
    private final IPurchaseOrderItemService purchaseOrderItemService;

    public AgriTaskServiceImpl(ISysUserService userService,
            IAgriTaskMaterialService taskMaterialService,
            IAgriTaskLogService taskLogService,
            MaterialInfoMapper materialInfoMapper,
            IMaterialStockLogService stockLogService,
            IPurchaseOrderService purchaseOrderService,
            IPurchaseOrderItemService purchaseOrderItemService) {
        this.userService = userService;
        this.taskMaterialService = taskMaterialService;
        this.taskLogService = taskLogService;
        this.materialInfoMapper = materialInfoMapper;
        this.stockLogService = stockLogService;
        this.purchaseOrderService = purchaseOrderService;
        this.purchaseOrderItemService = purchaseOrderItemService;
    }

    @Override
    public void createAutoTask(AgriTask task) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getCreateTime() == null) {
            task.setCreateTime(now);
        }
        task.setStatusV2(task.getNeedReview() != null && task.getNeedReview() == 1
                ? TaskStatusV2.PENDING_REVIEW
                : TaskStatusV2.PENDING_ACCEPT);
        task.setUpdateTime(now);
        task.setVersion(0);
        this.save(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTask(AgriTask task, LoginUser operator) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getCreateTime() == null) {
            task.setCreateTime(now);
        }
        String initStatus = task.getNeedReview() != null && task.getNeedReview() == 1
                ? TaskStatusV2.PENDING_REVIEW
                : TaskStatusV2.PENDING_ACCEPT;
        task.setStatusV2(initStatus);
        task.setAssigneeId(null);
        task.setAssignTime(null);
        task.setAssignBy(null);
        task.setAssignRemark(null);
        task.setAcceptTime(null);
        task.setAcceptBy(null);
        task.setRejectTime(null);
        task.setRejectBy(null);
        task.setRejectReason(null);
        task.setUpdateTime(now);
        task.setVersion(0);
        if (operator != null) {
            task.setCreateBy(operator.getUserId());
            task.setUpdateBy(operator.getUserId());
        }
        this.save(task);

        // Write task log
        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(task.getTaskId());
        log.setBatchId(task.getBatchId());
        log.setAction("create");
        log.setFromStatus(null);
        log.setToStatus(initStatus);
        log.setOperatorId(operator != null ? operator.getUserId() : null);
        log.setCreatedAt(now);
        taskLogService.save(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(TaskAssignDTO dto, LoginUser operator, String traceId) {
        if (dto == null || dto.getTaskId() == null || dto.getAssigneeId() == null) {
            throw new IllegalArgumentException("taskId 和 assigneeId 不能为空");
        }
        if (dto.getTaskId() <= 0 || dto.getAssigneeId() <= 0) {
            throw new IllegalArgumentException("taskId 和 assigneeId 必须大于 0");
        }
        if (operator == null || !operator.hasAnyRole("ADMIN", "FARM_OWNER")) {
            throw new RuntimeException("仅管理员可派单");
        }

        AgriTask task = mustGetTask(dto.getTaskId());
        ensureAssigneeValid(dto.getAssigneeId());

        // V1: 所有派单统一进入 pending_accept，工人需要接单确认
        boolean idempotent = TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())
                && Objects.equals(task.getAssigneeId(), dto.getAssigneeId());
        if (idempotent) {
            return;
        }
        if (!TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())) {
            throw new RuntimeException("仅待接单任务可派单");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.assignTask(
                dto.getTaskId(),
                dto.getAssigneeId(),
                operator.getUserId(),
                dto.getRemark(),
                now,
                now,
                TaskStatusV2.PENDING_ACCEPT,
                TaskStatusV2.PENDING_ACCEPT,
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && TaskStatusV2.PENDING_ACCEPT.equals(latest.getStatusV2())
                    && Objects.equals(latest.getAssigneeId(), dto.getAssigneeId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，派单失败");
        }

        // Write task log
        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(dto.getTaskId());
        log.setBatchId(task.getBatchId());
        log.setAction("assign");
        log.setFromStatus(TaskStatusV2.PENDING_ACCEPT);
        log.setToStatus(TaskStatusV2.PENDING_ACCEPT);
        log.setOperatorId(operator.getUserId());
        log.setTargetUserId(dto.getAssigneeId());
        log.setRemark(dto.getRemark());
        log.setTraceId(traceId);
        log.setCreatedAt(now);
        taskLogService.save(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTask(TaskAcceptDTO dto, LoginUser operator, String traceId) {
        if (dto == null || dto.getTaskId() == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        if (operator == null || !operator.hasRole(ROLE_WORKER)) {
            throw new RuntimeException("仅工人可接单");
        }

        AgriTask task = mustGetTask(dto.getTaskId());
        if (TaskStatusV2.IN_PROGRESS.equals(task.getStatusV2())
                && Objects.equals(task.getAcceptBy(), operator.getUserId())) {
            return;
        }
        if (!TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())) {
            throw new RuntimeException("仅待接单任务可接单");
        }
        if (!Objects.equals(task.getAssigneeId(), operator.getUserId())) {
            throw new RuntimeException("仅被派单工人可接单");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.acceptTask(
                dto.getTaskId(),
                operator.getUserId(),
                operator.getUserId(),
                now,
                now,
                TaskStatusV2.PENDING_ACCEPT,
                TaskStatusV2.IN_PROGRESS,
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && TaskStatusV2.IN_PROGRESS.equals(latest.getStatusV2())
                    && Objects.equals(latest.getAcceptBy(), operator.getUserId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，接单失败");
        }

        // Write task log
        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(dto.getTaskId());
        log.setBatchId(task.getBatchId());
        log.setAction("accept");
        log.setFromStatus(TaskStatusV2.PENDING_ACCEPT);
        log.setToStatus(TaskStatusV2.IN_PROGRESS);
        log.setOperatorId(operator.getUserId());
        log.setTraceId(traceId);
        log.setCreatedAt(now);
        taskLogService.save(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(TaskRejectDTO dto, LoginUser operator, String traceId) {
        if (dto == null || dto.getTaskId() == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        if (operator == null || !operator.hasRole(ROLE_WORKER)) {
            throw new RuntimeException("仅工人可拒单");
        }

        AgriTask task = mustGetTask(dto.getTaskId());
        if (TaskStatusV2.REJECTED_REASSIGN.equals(task.getStatusV2())
                && Objects.equals(task.getRejectBy(), operator.getUserId())) {
            return;
        }
        if (!TaskStatusV2.PENDING_ACCEPT.equals(task.getStatusV2())) {
            throw new RuntimeException("仅待接单任务可拒单");
        }
        if (!Objects.equals(task.getAssigneeId(), operator.getUserId())) {
            throw new RuntimeException("仅被派单工人可拒单");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.rejectTask(
                dto.getTaskId(),
                operator.getUserId(),
                operator.getUserId(),
                dto.getReason(),
                now,
                now,
                TaskStatusV2.PENDING_ACCEPT,
                TaskStatusV2.REJECTED_REASSIGN,
                safeVersion(task));

        if (updated == 0) {
            AgriTask latest = this.getById(dto.getTaskId());
            if (latest != null
                    && TaskStatusV2.REJECTED_REASSIGN.equals(latest.getStatusV2())
                    && Objects.equals(latest.getRejectBy(), operator.getUserId())) {
                return;
            }
            throw new RuntimeException("任务状态已变化，拒单失败");
        }

        // Write task log
        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(dto.getTaskId());
        log.setBatchId(task.getBatchId());
        log.setAction("reject");
        log.setFromStatus(TaskStatusV2.PENDING_ACCEPT);
        log.setToStatus(TaskStatusV2.REJECTED_REASSIGN);
        log.setOperatorId(operator.getUserId());
        log.setRemark(dto.getReason());
        log.setTraceId(traceId);
        log.setCreatedAt(now);
        taskLogService.save(log);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long taskId, LoginUser operator, String traceId) {
        if (taskId == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        if (operator == null || !operator.hasRole(ROLE_WORKER)) {
            throw new RuntimeException("仅工人可完成任务");
        }

        AgriTask task = mustGetTask(taskId);
        if (!TaskStatusV2.IN_PROGRESS.equals(task.getStatusV2())) {
            throw new RuntimeException("仅执行中任务可完成");
        }
        if (!Objects.equals(task.getAssigneeId(), operator.getUserId())) {
            throw new RuntimeException("仅被派单工人可完成任务");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. CAS update task status
        int updated = baseMapper.completeTask(
                taskId, operator.getUserId(), now, now,
                TaskStatusV2.IN_PROGRESS, TaskStatusV2.COMPLETED,
                safeVersion(task));
        if (updated == 0) {
            throw new RuntimeException("任务状态已变化，完成失败");
        }

        // 2. Write task log
        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(taskId);
        log.setBatchId(task.getBatchId());
        log.setAction("complete");
        log.setFromStatus(TaskStatusV2.IN_PROGRESS);
        log.setToStatus(TaskStatusV2.COMPLETED);
        log.setOperatorId(operator.getUserId());
        log.setTraceId(traceId);
        log.setCreatedAt(now);
        taskLogService.save(log);

        // 3. Deduct stock for each task material (actual_qty)
        LambdaQueryWrapper<AgriTaskMaterial> mw = new LambdaQueryWrapper<>();
        mw.eq(AgriTaskMaterial::getTaskId, taskId);
        List<AgriTaskMaterial> materials = taskMaterialService.list(mw);

        List<MaterialInfo> belowThreshold = new ArrayList<>();

        for (AgriTaskMaterial tm : materials) {
            BigDecimal qty = tm.getActualQty();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Optimistic lock retry
            boolean deducted = false;
            for (int i = 0; i < MAX_RETRY; i++) {
                MaterialInfo mat = materialInfoMapper.selectById(tm.getMaterialId());
                if (mat == null) {
                    throw new RuntimeException("物资不存在: " + tm.getMaterialId());
                }
                if (mat.getCurrentStock().compareTo(qty) < 0) {
                    throw new RuntimeException("库存不足: " + mat.getName()
                            + " 需要 " + qty + " 当前 " + mat.getCurrentStock());
                }
                int ver = mat.getVersion() == null ? 0 : mat.getVersion();
                int rows = materialInfoMapper.deductStock(tm.getMaterialId(), qty, ver);
                if (rows > 0) {
                    // Write stock log
                    MaterialStockLog sl = new MaterialStockLog();
                    sl.setMaterialId(tm.getMaterialId());
                    sl.setChangeType("OUT");
                    sl.setQty(qty.negate());
                    sl.setBeforeStock(mat.getCurrentStock());
                    sl.setAfterStock(mat.getCurrentStock().subtract(qty));
                    sl.setRelatedType("task");
                    sl.setRelatedId(taskId);
                    sl.setOperatorId(operator.getUserId());
                    sl.setCreatedAt(now);
                    stockLogService.save(sl);

                    // Check threshold
                    BigDecimal afterStock = mat.getCurrentStock().subtract(qty);
                    if (mat.getSafeThreshold() != null
                            && afterStock.compareTo(mat.getSafeThreshold()) < 0) {
                        mat.setCurrentStock(afterStock);
                        belowThreshold.add(mat);
                    }
                    deducted = true;
                    break;
                }
            }
            if (!deducted) {
                throw new RuntimeException("库存扣减并发冲突，请重试");
            }
        }

        // 4. Generate purchase drafts for below-threshold materials
        if (!belowThreshold.isEmpty()) {
            generatePurchaseDrafts(belowThreshold, operator.getUserId(), now);
        }
    }

    private void generatePurchaseDrafts(List<MaterialInfo> materials, Long operatorId, LocalDateTime now) {
        // Group by supplierId
        Map<Long, List<MaterialInfo>> grouped = new HashMap<>();
        for (MaterialInfo m : materials) {
            if (m.getSupplierId() == null) continue;
            grouped.computeIfAbsent(m.getSupplierId(), k -> new ArrayList<>()).add(m);
        }

        for (Map.Entry<Long, List<MaterialInfo>> entry : grouped.entrySet()) {
            Long supplierId = entry.getKey();

            // Idempotent: check if draft already exists for this supplier today
            LambdaQueryWrapper<PurchaseOrder> existCheck = new LambdaQueryWrapper<>();
            existCheck.eq(PurchaseOrder::getSupplierId, supplierId)
                    .eq(PurchaseOrder::getStatus, "draft")
                    .ge(PurchaseOrder::getCreatedAt, now.toLocalDate().atStartOfDay());
            if (purchaseOrderService.count(existCheck) > 0) {
                continue;
            }

            PurchaseOrder po = new PurchaseOrder();
            po.setOrderNo("PO-" + System.currentTimeMillis() + "-" + supplierId);
            po.setStatus("draft");
            po.setSupplierId(supplierId);
            po.setCreatedBy(operatorId);
            po.setCreatedAt(now);
            po.setUpdatedAt(now);
            po.setTotalAmount(BigDecimal.ZERO);
            purchaseOrderService.save(po);

            BigDecimal total = BigDecimal.ZERO;
            for (MaterialInfo m : entry.getValue()) {
                BigDecimal purchaseQty = m.getSuggestPurchaseQty() != null
                        ? m.getSuggestPurchaseQty()
                        : m.getSafeThreshold().subtract(m.getCurrentStock()).max(BigDecimal.ONE);
                BigDecimal unitPrice = m.getUnitPrice() != null ? m.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal lineAmount = purchaseQty.multiply(unitPrice);

                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setPurchaseOrderId(po.getId());
                item.setMaterialId(m.getMaterialId());
                item.setPurchaseQty(purchaseQty);
                item.setReceiveQty(BigDecimal.ZERO);
                item.setUnitPrice(unitPrice);
                item.setLineAmount(lineAmount);
                purchaseOrderItemService.save(item);
                total = total.add(lineAmount);
            }

            po.setTotalAmount(total);
            purchaseOrderService.updateById(po);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<AgriTask> reviewTask(Long taskId, boolean approved, String comment) {
        if (taskId == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        LoginUser operator = LoginUserContext.requireUser();
        if (!operator.hasAnyRole("ADMIN", "FARM_OWNER", "MANAGER")) {
            throw new RuntimeException("仅管理角色可复核任务");
        }

        AgriTask task = mustGetTask(taskId);
        String toStatus = approved ? TaskStatusV2.PENDING_ACCEPT : TaskStatusV2.REJECTED_REVIEW;
        TaskStatusV2.assertTransition(task.getStatusV2(), toStatus);

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.reviewTask(taskId, operator.getUserId(), now,
                task.getStatusV2(), toStatus, safeVersion(task));
        if (updated == 0) {
            throw new RuntimeException("任务状态已变化，复核失败");
        }

        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(taskId);
        log.setAction("review");
        log.setFromStatus(task.getStatusV2());
        log.setToStatus(toStatus);
        log.setOperatorId(operator.getUserId());
        log.setRemark(comment);
        log.setCreatedAt(now);
        taskLogService.save(log);

        return R.ok(this.getById(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<AgriTask> suspendTask(Long taskId, String reason) {
        if (taskId == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        LoginUser operator = LoginUserContext.requireUser();

        AgriTask task = mustGetTask(taskId);
        TaskStatusV2.assertTransition(task.getStatusV2(), TaskStatusV2.SUSPENDED);

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.suspendTask(taskId, operator.getUserId(), reason, now,
                task.getStatusV2(), TaskStatusV2.SUSPENDED, safeVersion(task));
        if (updated == 0) {
            throw new RuntimeException("任务状态已变化，暂停失败");
        }

        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(taskId);
        log.setAction("suspend");
        log.setFromStatus(task.getStatusV2());
        log.setToStatus(TaskStatusV2.SUSPENDED);
        log.setOperatorId(operator.getUserId());
        log.setRemark(reason);
        log.setCreatedAt(now);
        taskLogService.save(log);

        return R.ok(this.getById(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<AgriTask> resumeTask(Long taskId) {
        if (taskId == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        LoginUser operator = LoginUserContext.requireUser();

        AgriTask task = mustGetTask(taskId);
        TaskStatusV2.assertTransition(task.getStatusV2(), TaskStatusV2.IN_PROGRESS);

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.resumeTask(taskId, operator.getUserId(), now,
                task.getStatusV2(), TaskStatusV2.IN_PROGRESS, safeVersion(task));
        if (updated == 0) {
            throw new RuntimeException("任务状态已变化，恢复失败");
        }

        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(taskId);
        log.setAction("resume");
        log.setFromStatus(task.getStatusV2());
        log.setToStatus(TaskStatusV2.IN_PROGRESS);
        log.setOperatorId(operator.getUserId());
        log.setCreatedAt(now);
        taskLogService.save(log);

        return R.ok(this.getById(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<AgriTask> cancelTask(Long taskId, String reason) {
        if (taskId == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        LoginUser operator = LoginUserContext.requireUser();

        AgriTask task = mustGetTask(taskId);
        TaskStatusV2.assertTransition(task.getStatusV2(), TaskStatusV2.CANCELLED);

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.cancelTask(taskId, operator.getUserId(), reason, now,
                task.getStatusV2(), TaskStatusV2.CANCELLED, safeVersion(task));
        if (updated == 0) {
            throw new RuntimeException("任务状态已变化，取消失败");
        }

        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(taskId);
        log.setAction("cancel");
        log.setFromStatus(task.getStatusV2());
        log.setToStatus(TaskStatusV2.CANCELLED);
        log.setOperatorId(operator.getUserId());
        log.setRemark(reason);
        log.setCreatedAt(now);
        taskLogService.save(log);

        return R.ok(this.getById(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<AgriTask> reassignTask(Long taskId, Long newAssigneeId) {
        if (taskId == null || newAssigneeId == null) {
            throw new RuntimeException("taskId 和 newAssigneeId 不能为空");
        }
        LoginUser operator = LoginUserContext.requireUser();
        if (!operator.hasAnyRole("ADMIN", "FARM_OWNER")) {
            throw new RuntimeException("仅管理员可重新分配任务");
        }
        ensureAssigneeValid(newAssigneeId);

        AgriTask task = mustGetTask(taskId);
        TaskStatusV2.assertTransition(task.getStatusV2(), TaskStatusV2.PENDING_ACCEPT);

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.reassignTask(taskId, newAssigneeId, operator.getUserId(), now, now,
                task.getStatusV2(), TaskStatusV2.PENDING_ACCEPT, safeVersion(task));
        if (updated == 0) {
            throw new RuntimeException("任务状态已变化，重新分配失败");
        }

        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(taskId);
        log.setAction("reassign");
        log.setFromStatus(task.getStatusV2());
        log.setToStatus(TaskStatusV2.PENDING_ACCEPT);
        log.setOperatorId(operator.getUserId());
        log.setTargetUserId(newAssigneeId);
        log.setCreatedAt(now);
        taskLogService.save(log);

        return R.ok(this.getById(taskId));
    }

    @Override
    public void updateBasicInfo(TaskUpdateDTO dto, LoginUser operator) {
        if (dto == null || dto.getTaskId() == null) {
            throw new RuntimeException("taskId 不能为空");
        }
        AgriTask task = new AgriTask();
        task.setTaskId(dto.getTaskId());
        task.setTaskName(dto.getTaskName());
        task.setTaskType(dto.getTaskType());
        task.setPriority(dto.getPriority());
        task.setUpdateTime(LocalDateTime.now());
        if (operator != null) {
            task.setUpdateBy(operator.getUserId());
        }
        this.updateById(task);
    }

    @Override
    public void fillTaskUserNames(List<AgriTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Set<Long> userIds = tasks.stream()
                .map(AgriTask::getAssigneeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userNameMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(
                        SysUser::getUserId,
                        this::toDisplayName,
                        (a, b) -> a));
        for (AgriTask task : tasks) {
            task.setAssigneeName(resolveUserName(task.getAssigneeId(), userNameMap));
        }
    }

    private AgriTask mustGetTask(Long taskId) {
        AgriTask task = this.getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        return task;
    }

    /**
     * 校验被派单人：必须存在、启用、且拥有 WORKER 角色
     */
    private void ensureAssigneeValid(Long assigneeId) {
        SysUser user = userService.getById(assigneeId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("被派单人不存在或已停用");
        }
        Set<String> roleKeys = userService.getRoleKeys(assigneeId);
        if (roleKeys == null || !roleKeys.contains(ROLE_WORKER)) {
            throw new RuntimeException("仅支持派给 WORKER 角色");
        }
    }

    private int safeVersion(AgriTask task) {
        return task.getVersion() == null ? 0 : task.getVersion();
    }

    private String toDisplayName(SysUser user) {
        if (user == null) {
            return null;
        }
        if (StrUtil.isNotBlank(user.getRealName())) {
            return user.getRealName();
        }
        return StrUtil.isBlank(user.getUsername()) ? null : user.getUsername();
    }

    private String resolveUserName(Long userId, Map<Long, String> userNameMap) {
        if (userId == null) {
            return null;
        }
        return userNameMap.get(userId);
    }
}
