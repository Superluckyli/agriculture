package lizhuoer.agri.agri_system.module.report.service;

import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO;

import java.util.function.Consumer;

public interface IReportAiSummaryService {
    void stream(ReportAiSummaryRequestDTO request,
                Consumer<ReportAiStreamEventVO> eventConsumer,
                Runnable completionCallback,
                Consumer<Throwable> errorCallback);
}
