package lizhuoer.agri.agri_system.module.system.domain.vo;

import lombok.Data;

import java.util.Set;

/**
 * 登录响应 VO — 确保 password 等敏感字段不出现在响应中
 */
@Data
public class LoginResponseVO {
    private String token;
    private UserVO user;
    private Set<String> roles;

    @Data
    public static class UserVO {
        private Long userId;
        private String username;
        private String realName;
        private String phone;
        private String deptName;
        private Integer status;
    }
}
