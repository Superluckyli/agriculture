package lizhuoer.agri.agri_system.module.task.domain.enums;

import java.util.Map;
import java.util.Set;

/**
 * V1 任务状态常量 (status_v2 VARCHAR).
 */
public final class TaskStatusV2 {
    // 已创建：任务池待分配
    public static final String CREATED = "created";
    // 待核复：IoT 自动创建任务，根据优先级可能需要人工审核
    public static final String PENDING_REVIEW = "pending_review";
    // 待接单：已指派给工人，等待工人确认
    public static final String PENDING_ACCEPT = "pending_accept";
    // 执行中：工人已领取任务
    public static final String IN_PROGRESS = "in_progress";
    // 已完成：工人已提交任务且库存扣减完成
    public static final String COMPLETED = "completed";
    // 已拒单：工人拒绝后，系统标志该任务需要重派
    public static final String REJECTED_REASSIGN = "rejected_reassign";
    // 审核不通过：管理角色拒绝了待核复任务
    public static final String REJECTED_REVIEW = "rejected_review";
    // 已逾期：任务期限已过系统自动标记
    public static final String OVERDUE = "overdue";

    // 合法状态转换矩阵: from -> Set<to>
    public static final Map<String, Set<String>> TRANSITIONS = Map.of(
            CREATED, Set.of(PENDING_ACCEPT, OVERDUE),
            PENDING_REVIEW, Set.of(CREATED, REJECTED_REVIEW, OVERDUE),
            PENDING_ACCEPT, Set.of(IN_PROGRESS, REJECTED_REASSIGN, OVERDUE),
            IN_PROGRESS, Set.of(COMPLETED, OVERDUE),
            REJECTED_REASSIGN, Set.of(PENDING_ACCEPT, OVERDUE),
            REJECTED_REVIEW, Set.of(),
            COMPLETED, Set.of(),
            OVERDUE, Set.of());

    public static boolean isValidTransition(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        Set<String> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static void assertTransition(String from, String to) {
        if (!isValidTransition(from, to)) {
            throw new RuntimeException("非法状态转换: " + from + " -> " + to);
        }
    }

    private TaskStatusV2() {
    }
}
