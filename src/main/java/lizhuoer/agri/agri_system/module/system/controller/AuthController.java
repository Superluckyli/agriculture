package lizhuoer.agri.agri_system.module.system.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.JwtTokenUtil;
import lizhuoer.agri.agri_system.module.system.domain.SysRole;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.domain.SysUserRole;
import lizhuoer.agri.agri_system.module.system.domain.dto.LoginBody;
import lizhuoer.agri.agri_system.module.system.mapper.SysRoleMapper;
import lizhuoer.agri.agri_system.module.system.mapper.SysUserRoleMapper;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
public class AuthController {

    private static final String DEFAULT_ROLE_KEY = "WORKER";

    private final ISysUserService userService;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public AuthController(ISysUserService userService,
                          SysUserRoleMapper userRoleMapper,
                          SysRoleMapper roleMapper) {
        this.userService = userService;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginBody loginBody) {
        if (loginBody == null || StrUtil.isBlank(loginBody.getUsername()) || StrUtil.isBlank(loginBody.getPassword())) {
            return R.fail("用户名或密码不能为空");
        }

        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginBody.getUsername()));

        if (user == null) {
            return R.fail("用户不存在");
        }

        boolean passwordOk;
        try {
            passwordOk = BCrypt.checkpw(loginBody.getPassword(), user.getPassword());
        } catch (Exception e) {
            passwordOk = false;
        }
        if (!passwordOk) {
            return R.fail("密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            return R.fail("用户已被禁用");
        }

        String token = JwtTokenUtil.generateToken(user.getUserId(), user.getUsername());
        Set<String> roleKeys = userService.getRoleKeys(user.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        data.put("roles", roleKeys);
        return R.ok(data, "登录成功");
    }

    @PostMapping("/register")
    @Transactional
    public R<Void> register(@RequestBody SysUser user) {
        if (user == null || StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPassword())) {
            return R.fail("用户名或密码不能为空");
        }

        if (userService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername())) > 0) {
            return R.fail("账号已存在");
        }

        Long defaultRoleId = resolveDefaultRoleId();
        if (defaultRoleId == null) {
            return R.fail("系统配置异常，请联系管理员");
        }

        user.setUserId(null);
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        user.setStatus(1);
        user.setCreateTime(null);
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
}
