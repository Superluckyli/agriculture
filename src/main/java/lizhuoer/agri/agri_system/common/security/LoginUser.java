package lizhuoer.agri.agri_system.common.security;

import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
public class LoginUser {
    private final Long userId;
    private final String username;
    private final Set<String> roleKeys;

    public LoginUser(Long userId, String username, Set<String> roleKeys) {
        this.userId = userId;
        this.username = username;
        this.roleKeys = roleKeys == null ? Collections.emptySet() : new HashSet<>(roleKeys);
    }

    public boolean hasRole(String roleKey) {
        return roleKey != null && roleKeys.contains(roleKey.toUpperCase());
    }

    public boolean hasAnyRole(String... keys) {
        if (keys == null) {
            return false;
        }
        for (String key : keys) {
            if (hasRole(key)) {
                return true;
            }
        }
        return false;
    }
}
