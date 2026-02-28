package lizhuoer.agri.agri_system.module.task.domain.enums;

/**
 * Task status machine.
 */
public enum TaskStatus {
    PENDING_ASSIGN(0, "待分配"),
    PENDING_ACCEPT(1, "待接单"),
    ACCEPTED(2, "已接单"),
    COMPLETED(3, "已完成"),
    OVERDUE(4, "已逾期"),
    REJECTED(5, "已拒单");

    private final int code;
    private final String desc;

    TaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
