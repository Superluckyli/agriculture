package lizhuoer.agri.agri_system.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.JwtTokenUtil;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.domain.dto.LoginBody;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    private final ISysUserService userService;

    public AuthController(ISysUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginBody loginBody) {
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginBody.getUsername()));

        if (user == null) {
            return R.fail("用户不存在");
        }
        if (!loginBody.getPassword().equals(user.getPassword())) {
            return R.fail("密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            return R.fail("用户已被禁用");
        }

        String token = JwtTokenUtil.generateToken(user.getUserId(), user.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return R.ok(data, "登录成功");
    }

    @PostMapping("/register")
    public R<Void> register(@RequestBody SysUser user) {
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername())) > 0) {
            return R.fail("账号已存在");
        }
        user.setCreateTime(null);
        userService.save(user);
        return R.ok(null, "注册成功");
    }
}
