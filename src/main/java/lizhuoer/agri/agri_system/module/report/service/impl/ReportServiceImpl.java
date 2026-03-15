package lizhuoer.agri.agri_system.module.report.service.impl;

import lizhuoer.agri.agri_system.module.report.domain.ChartDataVO;
import lizhuoer.agri.agri_system.module.report.mapper.ReportMapper;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements IReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Override
    @Cacheable(value = "dashboard", key = "'data'")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();

        // 1. 作物分布 (饼图)
        List<Map<String, Object>> cropDist = reportMapper.countBatchByCrop();
        result.put("cropDistribution", cropDist);

        // 2. 任务趋势 (折线图)
        List<Map<String, Object>> taskTrendRaw = reportMapper.countTaskLast7Days();
        // 转换成 ECharts 需要的 xAxis, series 格式
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
}
