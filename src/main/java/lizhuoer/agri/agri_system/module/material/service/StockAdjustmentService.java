package lizhuoer.agri.agri_system.module.material.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.common.exception.BusinessException;
import lizhuoer.agri.agri_system.common.exception.ErrorCode;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.domain.StockAdjustment;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper;
import lizhuoer.agri.agri_system.module.material.mapper.StockAdjustmentMapper;
import lizhuoer.agri.agri_system.module.material.stocklog.domain.MaterialStockLog;
import lizhuoer.agri.agri_system.module.material.stocklog.service.IMaterialStockLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存调整审核服务
 */
@Service
public class StockAdjustmentService {

    private static final int MAX_RETRY = 3;

    private final StockAdjustmentMapper adjustmentMapper;
    private final MaterialInfoMapper materialInfoMapper;
    private final IMaterialStockLogService stockLogService;

    public StockAdjustmentService(StockAdjustmentMapper adjustmentMapper,
                                   MaterialInfoMapper materialInfoMapper,
                                   IMaterialStockLogService stockLogService) {
        this.adjustmentMapper = adjustmentMapper;
        this.materialInfoMapper = materialInfoMapper;
        this.stockLogService = stockLogService;
    }

    /**
     * 提交调整申请
     */
    public StockAdjustment submit(StockAdjustment adjustment) {
        if (materialInfoMapper.selectById(adjustment.getMaterialId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "物资不存在");
        }
        adjustment.setStatus("pending");
        adjustment.setCreatedAt(LocalDateTime.now());
        adjustmentMapper.insert(adjustment);
        return adjustment;
    }

    /**
     * 审批通过 — 执行库存变更
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long adjustmentId, Long reviewerId, String remark) {
        StockAdjustment adj = getAndValidatePending(adjustmentId);

        adj.setStatus("approved");
        adj.setReviewerId(reviewerId);
        adj.setReviewRemark(remark);
        adj.setReviewTime(LocalDateTime.now());
        adjustmentMapper.updateById(adj);

        applyAdjustment(adj, reviewerId);
    }

    /**
     * 审批拒绝
     */
    public void reject(Long adjustmentId, Long reviewerId, String remark) {
        StockAdjustment adj = getAndValidatePending(adjustmentId);

        adj.setStatus("rejected");
        adj.setReviewerId(reviewerId);
        adj.setReviewRemark(remark);
        adj.setReviewTime(LocalDateTime.now());
        adjustmentMapper.updateById(adj);
    }

    public List<StockAdjustment> listByStatus(String status) {
        LambdaQueryWrapper<StockAdjustment> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(StockAdjustment::getStatus, status);
        }
        qw.orderByDesc(StockAdjustment::getCreatedAt);
        return adjustmentMapper.selectList(qw);
    }

    private StockAdjustment getAndValidatePending(Long id) {
        StockAdjustment adj = adjustmentMapper.selectById(id);
        if (adj == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "调整申请不存在");
        }
        if (!"pending".equals(adj.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "该申请已处理");
        }
        return adj;
    }

    private void applyAdjustment(StockAdjustment adj, Long operatorId) {
        for (int i = 0; i < MAX_RETRY; i++) {
            MaterialInfo mat = materialInfoMapper.selectById(adj.getMaterialId());
            if (mat == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "物资不存在");
            }
            int ver = mat.getVersion() != null ? mat.getVersion() : 0;
            BigDecimal before = mat.getCurrentStock();
            BigDecimal after;
            int rows;

            switch (adj.getAdjustType()) {
                case "INCREASE":
                    rows = materialInfoMapper.addStock(adj.getMaterialId(), adj.getQty(), ver);
                    after = before.add(adj.getQty());
                    break;
                case "DECREASE":
                    if (before.compareTo(adj.getQty()) < 0) {
                        throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                                "库存不足，当前 " + before + "，调减 " + adj.getQty());
                    }
                    rows = materialInfoMapper.deductStock(adj.getMaterialId(), adj.getQty(), ver);
                    after = before.subtract(adj.getQty());
                    break;
                default:
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的调整类型: " + adj.getAdjustType());
            }

            if (rows > 0) {
                MaterialStockLog log = new MaterialStockLog();
                log.setMaterialId(adj.getMaterialId());
                log.setChangeType("ADJUST");
                log.setQty(adj.getQty());
                log.setBeforeStock(before);
                log.setAfterStock(after);
                log.setRelatedType("adjustment");
                log.setRelatedId(adj.getId());
                log.setOperatorId(operatorId);
                log.setRemark(adj.getReason());
                log.setCreatedAt(LocalDateTime.now());
                stockLogService.save(log);
                return;
            }

            try { Thread.sleep(20); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("库存操作被中断", e);
            }
        }
        throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_FAIL, "库存更新并发冲突，请重试");
    }
}
