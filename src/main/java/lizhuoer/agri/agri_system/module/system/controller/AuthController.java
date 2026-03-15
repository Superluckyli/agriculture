package lizhuoer.agri.agri_system.module.system.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.AuditLog;
import lizhuoer.agri.agri_system.common.security.JwtTokenUtil;
import lizhuoer.agri.agri_system.common.security.LoginThrottleService;
import lizhuoer.agri.agri_system.module.system.domain.SysRole;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.domain.SysUserRole;
import lizhuoer.agri.agri_system.module.system.domain.dto.LoginBody;
import lizhuoer.agri.agri_system.module.system.domain.dto.RegisterRequest;
import lizhuoer.agri.agri_system.module.system.domain.vo.LoginResponseVO;
import lizhuoer.agri.agri_system.module.system.mapper.SysRoleMapper;
import lizhuoer.agri.agri_system.module.system.mapper.SysUserRoleMapper;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
public class AuthController {

    private static final String DEFAULT_ROLE_KEY = "WORKER";

    private final ISysUserService userService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final LoginThrottleService throttleService;

    public AuthController(ISysUserService userService,
                          SysUserRoleMapper userRoleMapper,
                          SysRoleMapper roleMapper,
                          LoginThrottleService throttleService) {
        this.userService = userService;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.throttleService = throttleService;
    }

    @PostMapping("/login")
    @AuditLog(module = "认证", action = "LOGIN", target = "用户登录")
    public R<LoginResponseVO> login(@Valid @RequestBody LoginBody loginBody,
                                     HttpServletRequest request) {
        String ip = resolveClientIp(request);
        String username = loginBody.getUsername();

        // 限流检查: 5 次失败后锁定 15 分钟
        long lockedSeconds = throttleService.checkLocked(ip, username);
        if (lockedSeconds > 0) {
            return R.fail("登录尝试过于频繁，请 " + lockedSeconds + " 秒后重试");
        }

        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            throttleService.recordFailure(ip, username);
            return R.fail("用户不存在");
        }

        boolean passwordOk;
        try {
            passwordOk = BCrypt.checkpw(loginBody.getPassword(), user.getPassword());
        } catch (Exception e) {
            passwordOk = false;
        }
        if (!passwordOk) {
            throttleService.recordFailure(ip, username);
            return R.fail("密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            return R.fail("用户已被禁用");
        }

        // 登录成功，清除限流记录
        throttleService.clearRecord(ip, username);

        String token = JwtTokenUtil.generateToken(user.getUserId(), user.getUsername());
        Set<String> roleKeys = userService.getRoleKeys(user.getUserId());

        LoginResponseVO.UserVO userVO = new LoginResponseVO.UserVO();
        userVO.setUserId(user.getUserId());
        userVO.setUsername(user.getUsername());
        userVO.setRealName(user.getRealName());
        userVO.setPhone(user.getPhone());
        userVO.setDeptName(user.getDeptName());
        userVO.setStatus(user.getStatus());

        LoginResponseVO vo = new LoginResponseVO();
        vo.setToken(token);
        vo.setUser(userVO);
        vo.setRoles(roleKeys);

        return R.ok(vo, "登录成功");
    }

    @PostMapping("/register")
    @Transactional
    public R<Void> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername())) > 0) {
            return R.fail("账号已存在");
        }

        Long defaultRoleId = resolveDefaultRoleId();
        if (defaultRoleId == null) {
            return R.fail("系统配置异常，请联系管理员");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(BCrypt.hashpw(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setPhone(req.getPhone());
        user.setStatus(1);
        userService.save(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getUserId());
        userRole.setRoleId(defaultRoleId);
        userRoleMapper.insert(userRole);

        return R.ok(null, "注册成功");
    }

    private Long resolveDefaultRoleId() {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, DEFAULT_ROLE_KEY)
                .last("LIMIT 1"));
        return role != null ? role.getRoleId() : null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }
}
