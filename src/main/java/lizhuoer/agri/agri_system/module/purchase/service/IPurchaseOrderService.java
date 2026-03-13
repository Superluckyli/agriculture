package lizhuoer.agri.agri_system.module.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;

public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    /** 确认采购单: draft -> confirmed */
    PurchaseOrder confirmOrder(Long orderId);

    /** 取消采购单: draft -> cancelled */
    PurchaseOrder cancelOrder(Long orderId);

    /** CAS 状态更新，返回影响行数 */
    int casUpdateStatus(Long id, String fromStatus, String toStatus, Long confirmedBy, Integer version);

    /** 分页查询，支持按状态和供应商ID筛选 */
    Page<PurchaseOrder> listPage(Page<PurchaseOrder> page, Long supplierId, String status);
}
