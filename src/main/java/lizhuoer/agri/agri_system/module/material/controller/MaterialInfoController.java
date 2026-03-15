package lizhuoer.agri.agri_system.module.material.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lizhuoer.agri.agri_system.common.domain.PageResult;
import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.common.security.LoginUserContext;
import lizhuoer.agri.agri_system.common.security.RequirePermission;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.domain.StockAdjustment;
import lizhuoer.agri.agri_system.module.material.service.IMaterialInfoService;
import lizhuoer.agri.agri_system.module.material.service.StockAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/material/info")
public class MaterialInfoController {

    @Autowired
    private IMaterialInfoService materialInfoService;
    @Autowired
    private StockAdjustmentService adjustmentService;

    @GetMapping("/list")
    public R<PageResult<MaterialInfo>> list(@RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String name, String category, Long supplierId) {
        Page<MaterialInfo> page = new Page<>(pageNum, pageSize);
        return R.ok(PageResult.from(materialInfoService.listPage(page, name, category, supplierId)));
    }

    @GetMapping("/low-stock")
    public R<List<MaterialInfo>> lowStock() {
        return R.ok(materialInfoService.listLowStock());
    }

    @GetMapping("/all")
    public R<List<MaterialInfo>> all() {
        return R.ok(materialInfoService.listAll());
    }

    @PostMapping
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<Void> add(@Valid @RequestBody MaterialInfo info) {
        materialInfoService.addMaterial(info);
        return R.ok();
    }

    @PutMapping
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<Void> edit(@Valid @RequestBody MaterialInfo info) {
        materialInfoService.updateMaterial(info);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<Void> remove(@PathVariable Long[] ids) {
        materialInfoService.deleteMaterial(Arrays.asList(ids));
        return R.ok();
    }

    // ======================== 库存调整审核 ========================

    /**
     * 提交库存调整申请
     */
    @PostMapping("/adjustment")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<StockAdjustment> submitAdjustment(@RequestBody StockAdjustment adjustment) {
        adjustment.setApplicantId(LoginUserContext.requireUser().getUserId());
        return R.ok(adjustmentService.submit(adjustment));
    }

    /**
     * 查询调整申请列表
     */
    @GetMapping("/adjustment/list")
    @RequirePermission(roles = {"ADMIN", "FARM_OWNER"})
    public R<List<StockAdjustment>> listAdjustments(@RequestParam(required = false) String status) {
        return R.ok(adjustmentService.listByStatus(status));
    }

    /**
     * 审批通过
     */
    @PutMapping("/adjustment/{id}/approve")
    @RequirePermission(roles = {"ADMIN"})
    public R<Void> approveAdjustment(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body) {
        Long reviewerId = LoginUserContext.requireUser().getUserId();
        String remark = body != null ? body.get("remark") : null;
        adjustmentService.approve(id, reviewerId, remark);
        return R.ok();
    }

    /**
     * 审批拒绝
     */
    @PutMapping("/adjustment/{id}/reject")
    @RequirePermission(roles = {"ADMIN"})
    public R<Void> rejectAdjustment(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        Long reviewerId = LoginUserContext.requireUser().getUserId();
        String remark = body != null ? body.get("remark") : null;
        adjustmentService.reject(id, reviewerId, remark);
        return R.ok();
    }
}
