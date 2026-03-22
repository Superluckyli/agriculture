package lizhuoer.agri.agri_system.module.report.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CostAnalyticsVO {
    private ReportAnalyticsFilterDTO filterContext;
    private PurchaseTrendVO purchaseTrend;
    private List<MaterialCostTopItemVO> materialCostTopN;
    private List<CategoryCostShareItemVO> categoryCostShare;
    private List<AbnormalCostItemVO> abnormalCostItems;

    @Data
    public static class PurchaseTrendVO {
        private List<String> labels;
        private List<BigDecimal> amount;
        private List<Integer> orderCount;
    }

    @Data
    public static class MaterialCostTopItemVO {
        private Long materialId;
        private String name;
        private String category;
        private BigDecimal consumedQty;
        private BigDecimal cost;
    }

    @Data
    public static class CategoryCostShareItemVO {
        private String category;
        private BigDecimal cost;
        private BigDecimal percent;
    }

    @Data
    public static class AbnormalCostItemVO {
        private Long materialId;
        private String materialName;
        private String category;
        private BigDecimal cost;
        private BigDecimal consumedQty;
        private String supplierName;
        private String note;
    }
}
