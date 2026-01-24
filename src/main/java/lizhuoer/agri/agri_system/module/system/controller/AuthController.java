package lizhuoer.agri.agri_system.module.system.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.jwt.JWT;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.domain.dto.LoginBody;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private ISysUserService userService;

    private static final byte[] JWT_KEY = "AgriSystemKey_123456".getBytes(StandardCharsets.UTF_8);

    /**
     * 登录
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginBody loginBody) {
        // 1. 查账号
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginBody.getUsername()));

        if (user == null) {
            return R.fail("用户不存在");
        }

        // 2. 校验密码 (假设存的是明文或者简单加密，演示用 BCrypt.checkpw)
        // 实际项目: if (!BCrypt.checkpw(loginBody.getPassword(), user.getPassword()))
        // 演示简单点，直接比对（如果之前插入是明文）
        // 这里模拟: 如果数据库里是明文，这里就直接比对；如果是密文，就得用 verify
        // 为了方便，这里假定数据库存的是明文，或者你自己手动加了密。
        // 暂且用明文比对，后续再加强。
        if (!loginBody.getPassword().equals(user.getPassword())) {
            return R.fail("密码错误");
        }

        // 3. 生成 Token
        String token = JWT.create()
                .setPayload("userId", user.getUserId())
                .setPayload("username", user.getUsername())
                .setKey(JWT_KEY)
                .sign();

        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        map.put("user", user);

        return R.ok(map, "登录成功");
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public R<Void> register(@RequestBody SysUser user) {
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername())) > 0) {
            return R.fail("账号已存在");
        }
        // 实际项目应加密: user.setPassword(BCrypt.hashpw(user.getPassword()));
        user.setCreateTime(null);
        userService.save(user);
        return R.ok(null, "注册成功");
    }
}
