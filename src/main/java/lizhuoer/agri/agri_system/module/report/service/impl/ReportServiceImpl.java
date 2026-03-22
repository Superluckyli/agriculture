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
        ReportAnalyticsFilterDTO normalizedFilter = normalizeFilter(filter);
        LocalDateTime startAt = toStartAt(normalizedFilter);
        LocalDateTime endAt = toEndAtExclusive(normalizedFilter);

        Map<String, Object> taskSummary = reportMapper.countAnalyticsTaskSummary(
                startAt,
                endAt,
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId(),
                normalizedFilter.getAssigneeId());
        Map<String, Object> activeBatchSummary = reportMapper.countAnalyticsActiveBatchSummary(
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId());
        Map<String, Object> outputSummary = reportMapper.sumAnalyticsOutputSummary(
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId());
        Map<String, Object> costSummary = reportMapper.sumAnalyticsCostSummary(
                startAt,
                endAt,
                normalizedFilter.getMaterialCategory(),
                normalizedFilter.getSupplierId());

        ReportAnalyticsOverviewVO vo = new ReportAnalyticsOverviewVO();
        vo.setFilterContext(normalizedFilter);

        ReportAnalyticsOverviewVO.KpisVO kpis = new ReportAnalyticsOverviewVO.KpisVO();
        int assignedCount = getInt(taskSummary, "assignedCount");
        int completedCount = getInt(taskSummary, "completedCount");
        int onTimeCompletedCount = getInt(taskSummary, "onTimeCompletedCount");

        BigDecimal targetTotal = getBigDecimal(outputSummary, "targetTotal");
        BigDecimal actualTotal = getBigDecimal(outputSummary, "actualTotal");

        kpis.setTaskCompletionRate(toPercent(completedCount, assignedCount));
        kpis.setOnTimeExecutionRate(toPercent(onTimeCompletedCount, completedCount));
        kpis.setOverdueTaskCount(getInt(taskSummary, "overdueCount"));
        kpis.setActiveBatchCount(getInt(activeBatchSummary, "activeBatchCount"));
        kpis.setOutputAchievementRate(toPercent(actualTotal, targetTotal));
        kpis.setPurchaseAmount(getBigDecimal(costSummary, "purchaseAmount"));
        kpis.setMaterialCost(getBigDecimal(costSummary, "materialCost"));
        kpis.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setKpis(kpis);

        return vo;
    }

    @Override
    public TaskAnalyticsVO getTaskAnalyticsData(ReportAnalyticsFilterDTO filter) {
        ReportAnalyticsFilterDTO normalizedFilter = normalizeFilter(filter);
        LocalDateTime startAt = toStartAt(normalizedFilter);
        LocalDateTime endAt = toEndAtExclusive(normalizedFilter);
        String bucketFormat = resolveBucketFormat(normalizedFilter.getGranularity());

        List<Map<String, Object>> createdRows = reportMapper.countTaskCreatedTrend(
                startAt,
                endAt,
                bucketFormat,
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId(),
                normalizedFilter.getAssigneeId());
        List<Map<String, Object>> completedRows = reportMapper.countTaskCompletedTrend(
                startAt,
                endAt,
                bucketFormat,
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId(),
                normalizedFilter.getAssigneeId());
        List<Map<String, Object>> overdueRows = reportMapper.countTaskOverdueTrend(
                startAt,
                endAt,
                bucketFormat,
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId(),
                normalizedFilter.getAssigneeId());
        List<Map<String, Object>> statusRows = reportMapper.countTaskStatusDistribution(
                startAt,
                endAt,
                bucketFormat,
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId(),
                normalizedFilter.getAssigneeId());
        List<Map<String, Object>> assigneeRows = reportMapper.listTaskAssigneeRanking(
                startAt,
                endAt,
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId(),
                normalizedFilter.getAssigneeId());
        List<Map<String, Object>> abnormalRows = reportMapper.listAbnormalTasks(
                startAt,
                endAt,
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId(),
                normalizedFilter.getAssigneeId());

        List<String> trendLabels = mergeSortedLabels(createdRows, completedRows, overdueRows);
        Map<String, Integer> createdSeries = toCountMap(createdRows);
        Map<String, Integer> completedSeries = toCountMap(completedRows);
        Map<String, Integer> overdueSeries = toCountMap(overdueRows);

        TaskAnalyticsVO vo = new TaskAnalyticsVO();
        vo.setFilterContext(normalizedFilter);

        TaskAnalyticsVO.TrendVO trend = new TaskAnalyticsVO.TrendVO();
        trend.setLabels(trendLabels);
        trend.setCreated(toSeries(trendLabels, createdSeries));
        trend.setCompleted(toSeries(trendLabels, completedSeries));
        trend.setOverdue(toSeries(trendLabels, overdueSeries));
        vo.setTrend(trend);

        vo.setStatusDistribution(buildStatusDistribution(statusRows));
        vo.setAssigneeRanking(buildAssigneeRanking(assigneeRows));
        vo.setAbnormalTasks(buildAbnormalTasks(abnormalRows));
        return vo;
    }

    @Override
    public ProductionAnalyticsVO getProductionAnalyticsData(ReportAnalyticsFilterDTO filter) {
        ReportAnalyticsFilterDTO normalizedFilter = normalizeFilter(filter);
        String bucketFormat = resolveBucketFormat(normalizedFilter.getGranularity());

        List<Map<String, Object>> cropDistributionRows = reportMapper.listAnalyticsCropDistribution(
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId());
        List<Map<String, Object>> outputComparisonRows = reportMapper.listAnalyticsOutputComparison(
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId());
        List<Map<String, Object>> harvestTrendRows = reportMapper.countAnalyticsHarvestTrend(
                bucketFormat,
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId());
        List<Map<String, Object>> riskBatchRows = reportMapper.listAnalyticsRiskBatches(
                normalizedFilter.getFarmlandId(),
                normalizedFilter.getVarietyId());

        ProductionAnalyticsVO vo = new ProductionAnalyticsVO();
        vo.setFilterContext(normalizedFilter);
        vo.setCropDistribution(buildCropDistribution(cropDistributionRows));
        vo.setOutputComparison(buildOutputComparison(outputComparisonRows));

        ProductionAnalyticsVO.HarvestTrendVO harvestTrend = new ProductionAnalyticsVO.HarvestTrendVO();
        List<String> labels = harvestTrendRows == null ? Collections.emptyList() : harvestTrendRows.stream()
                .map(row -> Objects.toString(row.get("label"), ""))
                .filter(label -> !label.isBlank())
                .collect(Collectors.toList());
        harvestTrend.setLabels(labels);
        harvestTrend.setBatchCount(harvestTrendRows == null ? Collections.emptyList() : harvestTrendRows.stream()
                .map(row -> getInt(row, "batchCount"))
                .collect(Collectors.toList()));
        harvestTrend.setEstimatedOutput(harvestTrendRows == null ? Collections.emptyList() : harvestTrendRows.stream()
                .map(row -> getBigDecimal(row, "estimatedOutput"))
                .collect(Collectors.toList()));
        vo.setHarvestTrend(harvestTrend);

        vo.setRiskBatches(buildRiskBatches(riskBatchRows));
        return vo;
    }

    @Override
    public CostAnalyticsVO getCostAnalyticsData(ReportAnalyticsFilterDTO filter) {
        ReportAnalyticsFilterDTO normalizedFilter = normalizeFilter(filter);
        LocalDateTime startAt = toStartAt(normalizedFilter);
        LocalDateTime endAt = toEndAtExclusive(normalizedFilter);
        String bucketFormat = resolveBucketFormat(normalizedFilter.getGranularity());

        List<Map<String, Object>> purchaseTrendRows = reportMapper.sumAnalyticsPurchaseTrend(
                startAt,
                endAt,
                bucketFormat,
                normalizedFilter.getMaterialCategory(),
                normalizedFilter.getSupplierId());
        List<Map<String, Object>> materialCostRows = reportMapper.sumAnalyticsMaterialCostTopN(
                startAt,
                endAt,
                normalizedFilter.getMaterialCategory(),
                normalizedFilter.getSupplierId());
        List<Map<String, Object>> categoryCostRows = reportMapper.sumAnalyticsCategoryCostShare(
                startAt,
                endAt,
                normalizedFilter.getMaterialCategory(),
                normalizedFilter.getSupplierId());
        List<Map<String, Object>> abnormalCostRows = reportMapper.listAnalyticsAbnormalCostItems(
                startAt,
                endAt,
                normalizedFilter.getMaterialCategory(),
                normalizedFilter.getSupplierId());

        CostAnalyticsVO vo = new CostAnalyticsVO();
        vo.setFilterContext(normalizedFilter);

        CostAnalyticsVO.PurchaseTrendVO purchaseTrend = new CostAnalyticsVO.PurchaseTrendVO();
        purchaseTrend.setLabels(purchaseTrendRows == null ? Collections.emptyList() : purchaseTrendRows.stream()
                .map(row -> Objects.toString(row.get("label"), ""))
                .filter(label -> !label.isBlank())
                .collect(Collectors.toList()));
        purchaseTrend.setAmount(purchaseTrendRows == null ? Collections.emptyList() : purchaseTrendRows.stream()
                .map(row -> getBigDecimal(row, "amount"))
                .collect(Collectors.toList()));
        purchaseTrend.setOrderCount(purchaseTrendRows == null ? Collections.emptyList() : purchaseTrendRows.stream()
                .map(row -> getInt(row, "orderCount"))
                .collect(Collectors.toList()));
        vo.setPurchaseTrend(purchaseTrend);

        vo.setMaterialCostTopN(buildMaterialCostTopN(materialCostRows));
        vo.setCategoryCostShare(buildCategoryCostShare(categoryCostRows));
        vo.setAbnormalCostItems(buildAbnormalCostItems(abnormalCostRows));
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

    private ReportAnalyticsFilterDTO normalizeFilter(ReportAnalyticsFilterDTO filter) {
        ReportAnalyticsFilterDTO normalized = new ReportAnalyticsFilterDTO();
        LocalDate today = LocalDate.now();

        normalized.setStartDate(filter != null && filter.getStartDate() != null ? filter.getStartDate() : today.minusDays(29));
        normalized.setEndDate(filter != null && filter.getEndDate() != null ? filter.getEndDate() : today);
        normalized.setGranularity(filter != null && filter.getGranularity() != null && !filter.getGranularity().isBlank()
                ? filter.getGranularity() : "day");

        if (filter != null) {
            normalized.setFarmlandId(filter.getFarmlandId());
            normalized.setVarietyId(filter.getVarietyId());
            normalized.setAssigneeId(filter.getAssigneeId());
            normalized.setMaterialCategory(filter.getMaterialCategory());
            normalized.setSupplierId(filter.getSupplierId());
        }
        return normalized;
    }

    private LocalDateTime toStartAt(ReportAnalyticsFilterDTO filter) {
        return filter.getStartDate().atStartOfDay();
    }

    private LocalDateTime toEndAtExclusive(ReportAnalyticsFilterDTO filter) {
        return filter.getEndDate().plusDays(1).atStartOfDay();
    }

    private String resolveBucketFormat(String granularity) {
        if ("month".equalsIgnoreCase(granularity)) {
            return "%Y-%m";
        }
        if ("week".equalsIgnoreCase(granularity)) {
            return "%x-W%v";
        }
        return "%Y-%m-%d";
    }

    private int getInt(Map<String, Object> source, String key) {
        if (source == null) {
            return 0;
        }
        Object value = source.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private BigDecimal getBigDecimal(Map<String, Object> source, String key) {
        if (source == null) {
            return BigDecimal.ZERO;
        }
        Object value = source.get(key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    private BigDecimal toPercent(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal toPercent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 1, RoundingMode.HALF_UP);
    }

    @SafeVarargs
    private final List<String> mergeSortedLabels(List<Map<String, Object>>... rowsGroups) {
        TreeSet<String> labels = new TreeSet<>();
        for (List<Map<String, Object>> rows : rowsGroups) {
            if (rows == null) {
                continue;
            }
            for (Map<String, Object> row : rows) {
                Object label = row.get("label");
                if (label != null) {
                    labels.add(label.toString());
                }
            }
        }
        return new ArrayList<>(labels);
    }

    private Map<String, Integer> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Integer> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Object label = row.get("label");
            if (label != null) {
                result.put(label.toString(), getInt(row, "count"));
            }
        }
        return result;
    }

    private List<Integer> toSeries(List<String> labels, Map<String, Integer> countMap) {
        return labels.stream()
                .map(label -> countMap.getOrDefault(label, 0))
                .collect(Collectors.toList());
    }

    private TaskAnalyticsVO.StatusDistributionVO buildStatusDistribution(List<Map<String, Object>> rows) {
        TaskAnalyticsVO.StatusDistributionVO distribution = new TaskAnalyticsVO.StatusDistributionVO();
        List<String> labels = mergeSortedLabels(rows);
        distribution.setLabels(labels);

        Map<String, Map<String, Integer>> statusToLabelCount = new LinkedHashMap<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                String status = Objects.toString(row.get("status"), "");
                String label = Objects.toString(row.get("label"), "");
                if (status.isBlank() || label.isBlank()) {
                    continue;
                }
                statusToLabelCount.computeIfAbsent(status, unused -> new HashMap<>())
                        .put(label, getInt(row, "count"));
            }
        }

        List<TaskAnalyticsVO.StatusSeriesItemVO> series = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : statusToLabelCount.entrySet()) {
            TaskAnalyticsVO.StatusSeriesItemVO item = new TaskAnalyticsVO.StatusSeriesItemVO();
            item.setName(entry.getKey());
            item.setData(toSeries(labels, entry.getValue()));
            series.add(item);
        }
        distribution.setSeries(series);
        return distribution;
    }

    private List<TaskAnalyticsVO.AssigneeRankingItemVO> buildAssigneeRanking(List<Map<String, Object>> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            TaskAnalyticsVO.AssigneeRankingItemVO item = new TaskAnalyticsVO.AssigneeRankingItemVO();
            Object assigneeId = row.get("assigneeId");
            if (assigneeId instanceof Number) {
                item.setAssigneeId(((Number) assigneeId).longValue());
            }
            item.setAssigneeName(Objects.toString(row.get("assigneeName"), "未分配"));

            int assignedCount = getInt(row, "assignedCount");
            int completedCount = getInt(row, "completedCount");
            int onTimeCompletedCount = getInt(row, "onTimeCompletedCount");
            int overdueCount = getInt(row, "overdueCount");

            item.setAssignedCount(assignedCount);
            item.setCompletedCount(completedCount);
            item.setCompletionRate(toPercent(completedCount, assignedCount));
            item.setOnTimeRate(toPercent(onTimeCompletedCount, completedCount));
            item.setOverdueRate(toPercent(overdueCount, assignedCount));
            return item;
        }).collect(Collectors.toList());
    }

    private List<TaskAnalyticsVO.AbnormalTaskItemVO> buildAbnormalTasks(List<Map<String, Object>> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            TaskAnalyticsVO.AbnormalTaskItemVO item = new TaskAnalyticsVO.AbnormalTaskItemVO();
            Object taskId = row.get("taskId");
            if (taskId instanceof Number) {
                item.setTaskId(((Number) taskId).longValue());
            }
            item.setTaskName(Objects.toString(row.get("taskName"), ""));
            item.setAssigneeName(Objects.toString(row.get("assigneeName"), ""));
            item.setStatusV2(Objects.toString(row.get("statusV2"), ""));
            item.setDeadlineAt(Objects.toString(row.get("deadlineAt"), null));
            item.setRiskLevel(Objects.toString(row.get("riskLevel"), ""));
            item.setOverdueDays(getInt(row, "overdueDays"));
            return item;
        }).collect(Collectors.toList());
    }

    private List<ProductionAnalyticsVO.CropDistributionItemVO> buildCropDistribution(List<Map<String, Object>> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            ProductionAnalyticsVO.CropDistributionItemVO item = new ProductionAnalyticsVO.CropDistributionItemVO();
            Object varietyId = row.get("varietyId");
            if (varietyId instanceof Number) {
                item.setVarietyId(((Number) varietyId).longValue());
            }
            item.setCropVariety(Objects.toString(row.get("cropVariety"), ""));
            item.setBatchCount(getInt(row, "batchCount"));
            return item;
        }).collect(Collectors.toList());
    }

    private List<ProductionAnalyticsVO.OutputComparisonItemVO> buildOutputComparison(List<Map<String, Object>> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            ProductionAnalyticsVO.OutputComparisonItemVO item = new ProductionAnalyticsVO.OutputComparisonItemVO();
            Object batchId = row.get("batchId");
            if (batchId instanceof Number) {
                item.setBatchId(((Number) batchId).longValue());
            }
            item.setBatchNo(Objects.toString(row.get("batchNo"), ""));
            item.setCropVariety(Objects.toString(row.get("cropVariety"), ""));
            BigDecimal targetOutput = getBigDecimal(row, "targetOutput");
            BigDecimal actualOutput = getBigDecimal(row, "actualOutput");
            item.setTargetOutput(targetOutput);
            item.setActualOutput(actualOutput);
            item.setAchievementRate(toPercent(actualOutput, targetOutput));
            return item;
        }).collect(Collectors.toList());
    }

    private List<ProductionAnalyticsVO.RiskBatchItemVO> buildRiskBatches(List<Map<String, Object>> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            ProductionAnalyticsVO.RiskBatchItemVO item = new ProductionAnalyticsVO.RiskBatchItemVO();
            Object batchId = row.get("batchId");
            if (batchId instanceof Number) {
                item.setBatchId(((Number) batchId).longValue());
            }
            item.setBatchNo(Objects.toString(row.get("batchNo"), ""));
            item.setCropVariety(Objects.toString(row.get("cropVariety"), ""));
            item.setFarmlandName(Objects.toString(row.get("farmlandName"), ""));
            BigDecimal targetOutput = getBigDecimal(row, "targetOutput");
            BigDecimal actualOutput = getBigDecimal(row, "actualOutput");
            item.setTargetOutput(targetOutput);
            item.setActualOutput(actualOutput);
            item.setAchievementRate(toPercent(actualOutput, targetOutput));
            item.setEstimatedHarvestDate(Objects.toString(row.get("estimatedHarvestDate"), null));
            item.setStage(Objects.toString(row.get("stage"), ""));
            return item;
        }).collect(Collectors.toList());
    }

    private List<CostAnalyticsVO.MaterialCostTopItemVO> buildMaterialCostTopN(List<Map<String, Object>> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            CostAnalyticsVO.MaterialCostTopItemVO item = new CostAnalyticsVO.MaterialCostTopItemVO();
            Object materialId = row.get("materialId");
            if (materialId instanceof Number) {
                item.setMaterialId(((Number) materialId).longValue());
            }
            item.setName(Objects.toString(row.get("name"), ""));
            item.setCategory(Objects.toString(row.get("category"), ""));
            item.setConsumedQty(getBigDecimal(row, "consumedQty"));
            item.setCost(getBigDecimal(row, "cost"));
            return item;
        }).collect(Collectors.toList());
    }

    private List<CostAnalyticsVO.CategoryCostShareItemVO> buildCategoryCostShare(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        BigDecimal totalCost = rows.stream()
                .map(row -> getBigDecimal(row, "cost"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream().map(row -> {
            CostAnalyticsVO.CategoryCostShareItemVO item = new CostAnalyticsVO.CategoryCostShareItemVO();
            BigDecimal cost = getBigDecimal(row, "cost");
            item.setCategory(Objects.toString(row.get("category"), ""));
            item.setCost(cost);
            item.setPercent(toPercent(cost, totalCost));
            return item;
        }).collect(Collectors.toList());
    }

    private List<CostAnalyticsVO.AbnormalCostItemVO> buildAbnormalCostItems(List<Map<String, Object>> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        return rows.stream().map(row -> {
            CostAnalyticsVO.AbnormalCostItemVO item = new CostAnalyticsVO.AbnormalCostItemVO();
            Object materialId = row.get("materialId");
            if (materialId instanceof Number) {
                item.setMaterialId(((Number) materialId).longValue());
            }
            item.setMaterialName(Objects.toString(row.get("materialName"), ""));
            item.setCategory(Objects.toString(row.get("category"), ""));
            item.setCost(getBigDecimal(row, "cost"));
            item.setConsumedQty(getBigDecimal(row, "consumedQty"));
            item.setSupplierName(Objects.toString(row.get("supplierName"), ""));
            item.setNote(Objects.toString(row.get("note"), ""));
            return item;
        }).collect(Collectors.toList());
    }
}
