package lizhuoer.agri.agri_system.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lizhuoer.agri.agri_system.module.crop.batch.domain.AgriCropBatch;
import lizhuoer.agri.agri_system.module.crop.batch.mapper.AgriCropBatchMapper;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import lizhuoer.agri.agri_system.module.material.mapper.MaterialInfoMapper;
import lizhuoer.agri.agri_system.module.report.domain.ChartDataVO;
import lizhuoer.agri.agri_system.module.report.domain.CostAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO;
import lizhuoer.agri.agri_system.module.report.domain.ProductionAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsFilterDTO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAnalyticsOverviewVO;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO.*;
import lizhuoer.agri.agri_system.module.report.domain.TaskAnalyticsVO;
import lizhuoer.agri.agri_system.module.report.mapper.ReportMapper;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements IReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private AgriCropBatchMapper cropBatchMapper;

    @Autowired
    private MaterialInfoMapper materialInfoMapper;

    @Override
    @Cacheable(value = "dashboard", key = "'data'")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();

        // 1. 作物分布 (饼图)
        List<Map<String, Object>> cropDist = reportMapper.countBatchByCrop();
        result.put("cropDistribution", cropDist);

        // 2. 任务趋势 (折线图)
        List<Map<String, Object>> taskTrendRaw = reportMapper.countTaskLast7Days();
        List<String> dates = new ArrayList<>();
        List<Object> counts = new ArrayList<>();
        for (Map<String, Object> m : taskTrendRaw) {
            dates.add(m.get("date").toString());
            counts.add(m.get("count"));
        }
        result.put("taskTrend", new ChartDataVO(dates, counts));

        // 3. 实时环境 (仪表盘/列表)
        result.put("envMonitor", reportMapper.avgSensorValueLastHour());

        return result;
    }

    @Override
    @Cacheable(value = "dashboardV2", key = "'data'")
    public DashboardV2VO getDashboardV2Data() {
        DashboardV2VO vo = new DashboardV2VO();

        // 1. 任务完成度
        Map<String, Object> taskMap = reportMapper.countTodayTaskCompletion();
        TaskCompletionVO tc = new TaskCompletionVO();
        tc.setCompleted(((Number) taskMap.getOrDefault("completed", 0)).intValue());
        tc.setTotal(((Number) taskMap.getOrDefault("total", 0)).intValue());
        vo.setTaskCompletion(tc);

        // 2. 耕地统计
        Map<String, Object> farmMap = reportMapper.sumFarmlandStats();
        FarmlandStatsVO fs = new FarmlandStatsVO();
        BigDecimal totalArea = new BigDecimal(farmMap.getOrDefault("totalArea", 0).toString());
        BigDecimal activeArea = new BigDecimal(farmMap.getOrDefault("activeArea", 0).toString());
        fs.setTotalArea(totalArea);
        fs.setActiveArea(activeArea);
        fs.setUtilizationRate(totalArea.compareTo(BigDecimal.ZERO) > 0
                ? activeArea.multiply(BigDecimal.valueOf(100)).divide(totalArea, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setFarmlandStats(fs);

        // 3. 预警数量
        vo.setAlertCount(reportMapper.countActiveAlerts());

        // 4. 本月采购支出
        BigDecimal spending = reportMapper.sumMonthlySpending();
        vo.setMonthlySpending(spending != null ? spending : BigDecimal.ZERO);

        // 5. 作物生长进度
        vo.setCropProgress(buildCropProgress());

        // 6. 预警列表
        vo.setRecentAlerts(buildAlertList());

        // 7. 低库存物资
        vo.setLowStockMaterials(buildLowStockList());

        return vo;
    }

    @Override
    public ReportAnalyticsOverviewVO getAnalyticsOverviewData(ReportAnalyticsFilterDTO filter) {
        ReportAnalyticsOverviewVO vo = new ReportAnalyticsOverviewVO();
        vo.setFilterContext(filter);

        ReportAnalyticsOverviewVO.KpisVO kpis = new ReportAnalyticsOverviewVO.KpisVO();
        kpis.setTaskCompletionRate(BigDecimal.ZERO);
        kpis.setOnTimeExecutionRate(BigDecimal.ZERO);
        kpis.setOverdueTaskCount(0);
        kpis.setActiveBatchCount(0);
        kpis.setOutputAchievementRate(BigDecimal.ZERO);
        kpis.setPurchaseAmount(BigDecimal.ZERO);
        kpis.setMaterialCost(BigDecimal.ZERO);
        vo.setKpis(kpis);

        return vo;
    }

    @Override
    public TaskAnalyticsVO getTaskAnalyticsData(ReportAnalyticsFilterDTO filter) {
        TaskAnalyticsVO vo = new TaskAnalyticsVO();
        vo.setFilterContext(filter);

        TaskAnalyticsVO.TrendVO trend = new TaskAnalyticsVO.TrendVO();
        trend.setLabels(Collections.emptyList());
        trend.setCreated(Collections.emptyList());
        trend.setCompleted(Collections.emptyList());
        trend.setOverdue(Collections.emptyList());
        vo.setTrend(trend);

        TaskAnalyticsVO.StatusDistributionVO statusDistribution = new TaskAnalyticsVO.StatusDistributionVO();
        statusDistribution.setLabels(Collections.emptyList());
        statusDistribution.setSeries(Collections.emptyList());
        vo.setStatusDistribution(statusDistribution);

        vo.setAssigneeRanking(Collections.emptyList());
        vo.setAbnormalTasks(Collections.emptyList());
        return vo;
    }

    @Override
    public ProductionAnalyticsVO getProductionAnalyticsData(ReportAnalyticsFilterDTO filter) {
        ProductionAnalyticsVO vo = new ProductionAnalyticsVO();
        vo.setFilterContext(filter);
        vo.setCropDistribution(Collections.emptyList());
        vo.setOutputComparison(Collections.emptyList());

        ProductionAnalyticsVO.HarvestTrendVO harvestTrend = new ProductionAnalyticsVO.HarvestTrendVO();
        harvestTrend.setLabels(Collections.emptyList());
        harvestTrend.setBatchCount(Collections.emptyList());
        harvestTrend.setEstimatedOutput(Collections.emptyList());
        vo.setHarvestTrend(harvestTrend);

        vo.setRiskBatches(Collections.emptyList());
        return vo;
    }

    @Override
    public CostAnalyticsVO getCostAnalyticsData(ReportAnalyticsFilterDTO filter) {
        CostAnalyticsVO vo = new CostAnalyticsVO();
        vo.setFilterContext(filter);

        CostAnalyticsVO.PurchaseTrendVO purchaseTrend = new CostAnalyticsVO.PurchaseTrendVO();
        purchaseTrend.setLabels(Collections.emptyList());
        purchaseTrend.setAmount(Collections.emptyList());
        purchaseTrend.setOrderCount(Collections.emptyList());
        vo.setPurchaseTrend(purchaseTrend);

        vo.setMaterialCostTopN(Collections.emptyList());
        vo.setCategoryCostShare(Collections.emptyList());
        vo.setAbnormalCostItems(Collections.emptyList());
        return vo;
    }

    private List<CropProgressVO> buildCropProgress() {
        LambdaQueryWrapper<AgriCropBatch> qw = new LambdaQueryWrapper<>();
        qw.in(AgriCropBatch::getStatus, "in_progress", "not_started")
          .orderByDesc(AgriCropBatch::getPlantingDate)
          .last("LIMIT 10");
        List<AgriCropBatch> batches = cropBatchMapper.selectList(qw);
        LocalDate today = LocalDate.now();

        return batches.stream().map(b -> {
            CropProgressVO cp = new CropProgressVO();
            cp.setId(b.getId());
            cp.setBatchNo(b.getBatchNo());
            cp.setCropVariety(b.getCropVariety());
            cp.setFarmlandName(b.getFarmlandName());
            cp.setStage(b.getStage());
            cp.setStatus(b.getStatus());
            cp.setTargetOutput(b.getTargetOutput());
            cp.setActualOutput(b.getActualOutput());
            cp.setEstimatedHarvestDate(b.getEstimatedHarvestDate() != null
                    ? b.getEstimatedHarvestDate().toString() : null);

            // 计算进度百分比
            if (b.getPlantingDate() != null && b.getEstimatedHarvestDate() != null) {
                long totalDays = ChronoUnit.DAYS.between(b.getPlantingDate(), b.getEstimatedHarvestDate());
                long elapsed = ChronoUnit.DAYS.between(b.getPlantingDate(), today);
                int pct = totalDays > 0 ? (int) Math.min(100, Math.max(0, elapsed * 100 / totalDays)) : 0;
                cp.setProgressPercent(pct);
            }
            return cp;
        }).collect(Collectors.toList());
    }

    private List<AlertItemVO> buildAlertList() {
        List<Map<String, Object>> raw = reportMapper.listActiveAlerts();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        return raw.stream().map(m -> {
            AlertItemVO a = new AlertItemVO();
            String ruleName = Objects.toString(m.get("rule_name"), "未知规则");
            String sensorType = Objects.toString(m.get("sensor_type"), "");
            BigDecimal value = m.get("value") != null ? new BigDecimal(m.get("value").toString()) : BigDecimal.ZERO;
            int priority = m.get("priority") != null ? ((Number) m.get("priority")).intValue() : 3;

            a.setTitle(ruleName);
            a.setDescription(sensorType + " 当前值: " + value);
            a.setSensorType(sensorType);
            a.setLevel(priority <= 1 ? "CRITICAL" : priority == 2 ? "WARNING" : "INFO");

            Object ct = m.get("create_time");
            if (ct instanceof LocalDateTime) {
                a.setTime(((LocalDateTime) ct).format(fmt));
            } else if (ct != null) {
                a.setTime(ct.toString().substring(Math.max(0, ct.toString().length() - 8), ct.toString().length()));
            }
            return a;
        }).collect(Collectors.toList());
    }

    private List<LowStockVO> buildLowStockList() {
        LambdaQueryWrapper<MaterialInfo> qw = new LambdaQueryWrapper<>();
        qw.apply("current_stock <= safe_threshold")
          .eq(MaterialInfo::getStatus, 1);
        List<MaterialInfo> materials = materialInfoMapper.selectList(qw);

        return materials.stream().map(mi -> {
            LowStockVO ls = new LowStockVO();
            ls.setMaterialId(mi.getMaterialId());
            ls.setName(mi.getName());
            ls.setCategory(mi.getCategory());
            ls.setUnit(mi.getUnit());
            ls.setCurrentStock(mi.getCurrentStock());
            ls.setSafeThreshold(mi.getSafeThreshold());
            ls.setUnitPrice(mi.getUnitPrice());

            // 健康度
            if (mi.getSafeThreshold() != null && mi.getSafeThreshold().compareTo(BigDecimal.ZERO) > 0) {
                int hp = mi.getCurrentStock().multiply(BigDecimal.valueOf(100))
                        .divide(mi.getSafeThreshold(), 0, RoundingMode.HALF_UP).intValue();
                ls.setHealthPercent(Math.min(100, Math.max(0, hp)));
            }

            // 状态判定
            if (mi.getSafeThreshold() != null && mi.getCurrentStock()
                    .compareTo(mi.getSafeThreshold().multiply(BigDecimal.valueOf(0.3))) <= 0) {
                ls.setStatus("CRITICAL");
            } else {
                ls.setStatus("LOW_STOCK");
            }
            return ls;
        }).collect(Collectors.toList());
    }
}
