package lizhuoer.agri.agri_system.module.purchase.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrderItem;
import lizhuoer.agri.agri_system.module.purchase.mapper.PurchaseOrderItemMapper;
import lizhuoer.agri.agri_system.module.purchase.service.IPurchaseOrderItemService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderItemServiceImpl extends ServiceImpl<PurchaseOrderItemMapper, PurchaseOrderItem> implements IPurchaseOrderItemService {
}
