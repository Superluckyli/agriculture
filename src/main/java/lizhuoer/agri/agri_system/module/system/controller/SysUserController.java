package lizhuoer.agri.agri_system.module.system.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUser;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.common.security.AuditLog;
import lizhuoer.agri.agri_system.common.security.RequirePermission;
import lizhuoer.agri.agri_system.module.system.domain.SysRole;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.domain.SysUserRole;
import lizhuoer.agri.agri_system.module.system.mapper.SysRoleMapper;
import lizhuoer.agri.agri_system.module.system.mapper.SysUserRoleMapper;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.module.system.domain.dto.PasswordChangeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户管理 Controller
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    /**
     * 分页查询用户（附带角色信息）
     */
    @GetMapping("/list")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<PageResult<SysUser>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            SysUser user) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(user.getUsername()), SysUser::getUsername, user.getUsername())
                .like(StrUtil.isNotBlank(user.getRealName()), SysUser::getRealName, user.getRealName())
                .eq(user.getStatus() != null, SysUser::getStatus, user.getStatus())
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userService.page(page, wrapper);

        fillUserRoles(result.getRecords());

        return R.ok(PageResult.from(result));
    }

    /**
     * 获取详细信息
     */
    @GetMapping("/{userId}")
    public R<SysUser> getInfo(@PathVariable Long userId) {
        return R.ok(userService.getById(userId));
    }

    /**
     * 新增用户
     */
    @PostMapping
    @RequirePermission(roles = {"ADMIN"})
    @AuditLog(module = "系统管理", action = "CREATE", target = "用户")
    public R<Void> add(@RequestBody SysUser user) {
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername())) > 0) {
            return R.fail("新增用户'" + user.getUsername() + "'失败，账号已存在");
        }
        user.setUserId(null);
        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(hashPassword(user.getPassword()));
        }
        user.setCreateTime(null);
        userService.save(user);
        return R.ok();
    }

    /**
     * 修改用户
     */
    @PutMapping
    @RequirePermission(roles = {"ADMIN"})
    public R<Void> edit(@RequestBody SysUser user) {
        if (user.getPassword() != null) {
            if (StrUtil.isBlank(user.getPassword())) {
                user.setPassword(null);
            } else {
                user.setPassword(hashPassword(user.getPassword()));
            }
        }
        userService.updateById(user);
        return R.ok();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userIds}")
    @RequirePermission(roles = {"ADMIN"})
    @AuditLog(module = "系统管理", action = "DELETE", target = "用户")
    public R<Void> remove(@PathVariable Long[] userIds) {
        List<Long> ids = Arrays.asList(userIds);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, ids));
        userService.removeBatchByIds(ids);
        return R.ok();
    }

    // ==================== 角色分配 ====================

    /**
     * 获取用户已分配的角色ID列表
     */
    @GetMapping("/{userId}/roles")
    @RequirePermission(roles = {"ADMIN"})
    public R<List<Long>> getUserRoles(@PathVariable Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)
        ).stream().map(SysUserRole::getRoleId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        return R.ok(roleIds);
    }

    /**
     * 分配角色（全量替换）
     */
    @PutMapping("/{userId}/roles")
    @Transactional
    @RequirePermission(roles = {"ADMIN"})
    @AuditLog(module = "系统管理", action = "ASSIGN", target = "用户角色")
    public R<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        if (userService.getById(userId) == null) {
            return R.fail("用户不存在");
        }

        List<Long> distinctRoleIds = roleIds == null
                ? Collections.emptyList()
                : roleIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

        if (!distinctRoleIds.isEmpty()) {
            List<SysRole> existingRoles = roleMapper.selectBatchIds(distinctRoleIds);
            Set<Long> existingIds = existingRoles.stream()
                    .map(SysRole::getRoleId).collect(Collectors.toSet());
            List<Long> missing = distinctRoleIds.stream()
                    .filter(id -> !existingIds.contains(id)).collect(Collectors.toList());
            if (!missing.isEmpty()) {
                return R.fail("角色不存在: " + missing);
            }
        }

        userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));

        for (Long roleId : distinctRoleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        return R.ok(null, "角色分配成功");
    }

    // ==================== 个人中心 ====================

    /**
     * 更新个人资料 (realName / phone / deptName)
     */
    @PutMapping("/profile")
    public R<SysUser> updateProfile(@RequestBody SysUser dto) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return R.fail(401, "请先登录");
        }

        SysUser existing = userService.getById(loginUser.getUserId());
        if (existing == null) {
            return R.fail("用户不存在");
        }

        SysUser update = new SysUser();
        update.setUserId(loginUser.getUserId());
        if (dto.getRealName() != null) update.setRealName(dto.getRealName());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getDeptName() != null) update.setDeptName(dto.getDeptName());
        userService.updateById(update);

        SysUser updated = userService.getById(loginUser.getUserId());
        updated.setPassword(null);
        return R.ok(updated);
    }

    /**
     * 修改密码 (需验证旧密码)
     */
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req) {
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            return R.fail(401, "请先登录");
        }

        SysUser existing = userService.getById(loginUser.getUserId());
        if (existing == null) {
            return R.fail("用户不存在");
        }

        if (!BCrypt.checkpw(req.getOldPassword(), existing.getPassword())) {
            return R.fail("旧密码不正确");
        }

        SysUser update = new SysUser();
        update.setUserId(loginUser.getUserId());
        update.setPassword(BCrypt.hashpw(req.getNewPassword()));
        userService.updateById(update);
        return R.ok(null, "密码修改成功");
    }

    // ==================== 私有方法 ====================

    /**
     * 批量填充用户角色信息
     */
    private void fillUserRoles(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream().map(SysUser::getUserId).collect(Collectors.toList());

        // 批量查询用户-角色关联
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));

        if (userRoles.isEmpty()) {
            users.forEach(u -> {
                u.setRoleNames(Collections.emptyList());
                u.setRoleIds(Collections.emptyList());
            });
            return;
        }

        // 收集所有角色ID并查询角色详情
        Set<Long> allRoleIds = userRoles.stream()
                .map(SysUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, SysRole> roleMap = roleMapper.selectBatchIds(allRoleIds)
                .stream().collect(Collectors.toMap(SysRole::getRoleId, r -> r));

        // 按用户分组
        Map<Long, List<SysUserRole>> groupByUser = userRoles.stream()
                .collect(Collectors.groupingBy(SysUserRole::getUserId));

        for (SysUser u : users) {
            List<SysUserRole> urs = groupByUser.getOrDefault(u.getUserId(), Collections.emptyList());
            List<Long> rids = urs.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            List<String> rnames = urs.stream()
                    .map(ur -> {
                        SysRole r = roleMap.get(ur.getRoleId());
                        return r != null ? r.getRoleKey() : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            u.setRoleIds(rids);
            u.setRoleNames(rnames);
        }
    }

    private String hashPassword(String password) {
        if (password != null && password.startsWith("$2")) {
            return password;
        }
        return BCrypt.hashpw(password);
    }

}
