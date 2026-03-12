package lizhuoer.agri.agri_system.module.task.domain.enums;

import java.util.Map;
import java.util.Set;

/**
 * V1 任务状态常量 (status_v2 VARCHAR).
 */
public final class TaskStatusV2 {
    public static final String PENDING_REVIEW    = "pending_review";
    public static final String PENDING_ACCEPT    = "pending_accept";
    public static final String IN_PROGRESS       = "in_progress";
    public static final String COMPLETED         = "completed";
    public static final String REJECTED_REASSIGN = "rejected_reassign";
    public static final String REJECTED_REVIEW   = "rejected_review";
    public static final String SUSPENDED         = "suspended";
    public static final String OVERDUE           = "overdue";
    public static final String CANCELLED         = "cancelled";

    /** 合法状态转换矩阵: from -> Set<to> */
    public static final Map<String, Set<String>> TRANSITIONS = Map.of(
            PENDING_REVIEW, Set.of(PENDING_ACCEPT, REJECTED_REVIEW, CANCELLED),
            PENDING_ACCEPT, Set.of(IN_PROGRESS, REJECTED_REASSIGN, CANCELLED, PENDING_ACCEPT),
            IN_PROGRESS, Set.of(COMPLETED, SUSPENDED),
            SUSPENDED, Set.of(IN_PROGRESS, CANCELLED),
            REJECTED_REASSIGN, Set.of(PENDING_ACCEPT)
    );

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

    private TaskStatusV2() {}
}
