package lizhuoer.agri.agri_system.module.report.controller;

import lizhuoer.agri.agri_system.common.domain.R;
import lizhuoer.agri.agri_system.module.report.domain.DashboardV2VO;
import lizhuoer.agri.agri_system.module.report.service.IReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private IReportService reportService;

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(reportService.getDashboardData());
    }

    @GetMapping("/dashboard/v2")
    public R<DashboardV2VO> dashboardV2() {
        return R.ok(reportService.getDashboardV2Data());
    }
}
