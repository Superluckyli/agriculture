package lizhuoer.agri.agri_system.module.task.material.controller;

import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.task.material.domain.AgriTaskMaterial;
import lizhuoer.agri.agri_system.module.task.material.service.IAgriTaskMaterialService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/task")
public class AgriTaskMaterialController {

    private final IAgriTaskMaterialService taskMaterialService;

    public AgriTaskMaterialController(IAgriTaskMaterialService taskMaterialService) {
        this.taskMaterialService = taskMaterialService;
    }

    @PostMapping("/{taskId}/materials")
    public R<List<AgriTaskMaterial>> addMaterials(@PathVariable Long taskId,
            @RequestBody List<AgriTaskMaterial> items) {
        return taskMaterialService.addMaterials(taskId, items);
    }

    @PutMapping("/material/{id}")
    public R<AgriTaskMaterial> updateActualQty(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Object qtyObj = body.get("actualQty");
        if (qtyObj == null) {
            throw new IllegalArgumentException("actualQty 不能为空");
        }
        java.math.BigDecimal actualQty = new java.math.BigDecimal(qtyObj.toString());
        String deviationReason = (String) body.get("deviationReason");
        return taskMaterialService.updateActualQty(id, actualQty, deviationReason);
    }

    @GetMapping("/{taskId}/materials")
    public R<List<AgriTaskMaterial>> listByTaskId(@PathVariable Long taskId) {
        return taskMaterialService.listByTaskId(taskId);
    }
}
