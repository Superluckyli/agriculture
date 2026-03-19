package lizhuoer.agri.agri_system.module.report.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardV2VO {
    private TaskCompletionVO taskCompletion;
    private FarmlandStatsVO farmlandStats;
    private int alertCount;
    private BigDecimal monthlySpending;
    private List<CropProgressVO> cropProgress;
    private List<AlertItemVO> recentAlerts;
    private List<LowStockVO> lowStockMaterials;

    @Data
    public static class TaskCompletionVO {
        private int completed;
        private int total;
    }

    @Data
    public static class FarmlandStatsVO {
        private BigDecimal totalArea;
        private BigDecimal activeArea;
        private BigDecimal utilizationRate;
    }

    @Data
    public static class CropProgressVO {
        private Long id;
        private String batchNo;
        private String cropVariety;
        private String farmlandName;
        private int progressPercent;
        private String stage;
        private String status;
        private BigDecimal targetOutput;
        private BigDecimal actualOutput;
        private String estimatedHarvestDate;
    }

    @Data
    public static class AlertItemVO {
        private String title;
        private String description;
        private String level;
        private String time;
        private String sensorType;
    }

    @Data
    public static class LowStockVO {
        private Long materialId;
        private String name;
        private String category;
        private String unit;
        private BigDecimal currentStock;
        private BigDecimal safeThreshold;
        private BigDecimal unitPrice;
        private int healthPercent;
        private String status;
    }
}
