package lizhuoer.agri.agri_system.module.iot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.RequirePermission;
import lizhuoer.agri.agri_system.module.iot.domain.AgriTaskRule;
import lizhuoer.agri.agri_system.module.iot.service.IAgriTaskRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/iot/rule")
public class AgriTaskRuleController {

    @Autowired
    private IAgriTaskRuleService ruleService;

    @GetMapping("/list")
    public R<PageResult<AgriTaskRule>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<AgriTaskRule> page = new Page<>(pageNum, pageSize);
        return R.ok(PageResult.from(ruleService.page(page)));
    }

    @PostMapping
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<Void> add(@Valid @RequestBody AgriTaskRule rule) {
        ruleService.save(rule);
        return R.ok();
    }

    @PutMapping
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<Void> edit(@Valid @RequestBody AgriTaskRule rule) {
        ruleService.updateById(rule);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<Void> remove(@PathVariable Long id) {
        ruleService.removeById(id);
        return R.ok();
    }
}
