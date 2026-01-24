package lizhuoer.agri.agri_system.module.system.controller;

import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.system.domain.SysMenu;
import lizhuoer.agri.agri_system.module.system.service.ISysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取菜单列表 (不分页，通常是树形结构，这里简化直接返回列表)
     */
    @GetMapping("/list")
    public R<List<SysMenu>> list() {
        return R.ok(menuService.list());
    }

    @PostMapping
    public R<Void> add(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @PutMapping
    public R<Void> edit(@RequestBody SysMenu menu) {
        menuService.updateById(menu);
        return R.ok();
    }

    @DeleteMapping("/{menuId}")
    public R<Void> remove(@PathVariable Long menuId) {
        menuService.removeById(menuId);
        return R.ok();
    }
}
