package lizhuoer.agri.agri_system.common.security;

public final class LoginUserContext {
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser requireUser() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new RuntimeException("请先登录");
        }
        return loginUser;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
