package lizhuoer.agri.agri_system.module.task.domain.enums;

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

    private TaskStatusV2() {}
}
