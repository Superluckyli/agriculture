package lizhuoer.agri.agri_system.module.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.PurchaseOrderCreateRequest;
import lizhuoer.agri.agri_system.module.purchase.domain.dto.PurchaseOrderItemCreateRequest;

import java.util.List;

public interface IPurchaseOrderService extends IService<PurchaseOrder> {

    /** 事务性创建采购单（含明细） */
    PurchaseOrder createOrder(PurchaseOrderCreateRequest req);

    /** 确认采购单: draft -> confirmed */
    PurchaseOrder confirmOrder(Long orderId);

    /** 取消采购单: draft/confirmed -> cancelled */
    PurchaseOrder cancelOrder(Long orderId);

    /** 付款: confirmed -> paid (事务内同时创建 PaymentRecord) */
    void payOrder(Long id, String payMethod, Long operatorId);

    /** CAS 状态更新，返回影响行数 */
    int casUpdateStatus(Long id, String fromStatus, String toStatus, Long confirmedBy, Integer version);

    /** 分页查询，支持按状态和供应商ID筛选 */
    Page<PurchaseOrder> listPage(Page<PurchaseOrder> page, Long supplierId, String status);

    void deleteOrders(List<Long> ids);

    /** 重算订单总金额 */
    void recalcTotalAmount(Long orderId);

    /** 批量替换明细（删旧插新 + 重算总额） */
    void replaceItems(Long orderId, List<PurchaseOrderItemCreateRequest> items);
}
