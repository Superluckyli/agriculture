package lizhuoer.agri.agri_system.common.exception;

/**
 * 业务错误码枚举 — 统一错误标识，前端可据此做精确提示
 */
public enum ErrorCode {

    // --- 通用 ---
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // --- 认证 ---
    LOGIN_LOCKED(1001, "登录尝试过于频繁"),
    INVALID_CREDENTIALS(1002, "用户名或密码错误"),
    ACCOUNT_DISABLED(1003, "账户已被禁用"),

    // --- 业务 ---
    DUPLICATE_ENTRY(2001, "数据已存在"),
    OPTIMISTIC_LOCK_FAIL(2002, "数据已被修改，请刷新后重试"),
    INSUFFICIENT_STOCK(2003, "库存不足"),
    INVALID_STATE_TRANSITION(2004, "状态变更不合法");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
