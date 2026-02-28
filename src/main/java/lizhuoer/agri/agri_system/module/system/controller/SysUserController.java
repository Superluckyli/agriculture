package lizhuoer.agri.agri_system.module.system.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.domain.SysUserRole;
import lizhuoer.agri.agri_system.module.system.mapper.SysUserRoleMapper;
import lizhuoer.agri.agri_system.module.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 Controller (简化版)
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    /**
     * 分页查询用户
     */
    @GetMapping("/list")
    public R<Page<SysUser>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            SysUser user) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(user.getUsername()), SysUser::getUsername, user.getUsername())
                .like(StrUtil.isNotBlank(user.getRealName()), SysUser::getRealName, user.getRealName())
                .eq(user.getStatus() != null, SysUser::getStatus, user.getStatus())
                .orderByDesc(SysUser::getCreateTime);
        return R.ok(userService.page(page, wrapper));
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
    public R<Void> add(@RequestBody SysUser user) {
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername())) > 0) {
            return R.fail("新增用户'" + user.getUsername() + "'失败，账号已存在");
        }
        user.setCreateTime(null); // use db default
        userService.save(user);
        return R.ok();
    }

    /**
     * 修改用户
     */
    @PutMapping
    public R<Void> edit(@RequestBody SysUser user) {
        userService.updateById(user);
        return R.ok();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userIds}")
    public R<Void> remove(@PathVariable Long[] userIds) {
        java.util.List<Long> ids = java.util.Arrays.asList(userIds);
        // 先删除用户-角色关联记录
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, ids));
        // 再删除用户
        userService.removeBatchByIds(ids);
        return R.ok();
    }
}
