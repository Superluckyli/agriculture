package lizhuoer.agri.agri_system.module.report.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductionAnalyticsVO {
    private ReportAnalyticsFilterDTO filterContext;
    private List<CropDistributionItemVO> cropDistribution;
    private List<OutputComparisonItemVO> outputComparison;
    private HarvestTrendVO harvestTrend;
    private List<RiskBatchItemVO> riskBatches;

    @Data
    public static class CropDistributionItemVO {
        private Long varietyId;
        private String cropVariety;
        private Integer batchCount;
    }

    @Data
    public static class OutputComparisonItemVO {
        private Long batchId;
        private String batchNo;
        private String cropVariety;
        private BigDecimal targetOutput;
        private BigDecimal actualOutput;
        private BigDecimal achievementRate;
    }

    @Data
    public static class HarvestTrendVO {
        private List<String> labels;
        private List<Integer> batchCount;
        private List<BigDecimal> estimatedOutput;
    }

    @Data
    public static class RiskBatchItemVO {
        private Long batchId;
        private String batchNo;
        private String cropVariety;
        private String farmlandName;
        private BigDecimal targetOutput;
        private BigDecimal actualOutput;
        private BigDecimal achievementRate;
        private String estimatedHarvestDate;
        private String stage;
    }
}
