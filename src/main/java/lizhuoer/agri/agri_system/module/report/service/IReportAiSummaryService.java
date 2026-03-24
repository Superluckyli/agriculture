package lizhuoer.agri.agri_system.module.report.service;

import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO;

import java.util.function.Consumer;

public interface IReportAiSummaryService {
    /**
     * 按请求流式输出报表 AI 事件。
     * <p>
     * 回调约定：
     * <ul>
     *   <li>{@code eventConsumer} 按顺序接收 section / evidence / done 事件。</li>
     *   <li>{@code completionCallback} 表示 provider 侧文本已经成功生成完毕，
     *   但下游 transport 仍需等到终止 {@code done} 事件发出后才能关闭。</li>
     *   <li>{@code errorCallback} 是唯一失败终止路径，调用方应把它视为单次失败信号。</li>
     * </ul>
     */
    void stream(ReportAiSummaryRequestDTO request,
                Consumer<ReportAiStreamEventVO> eventConsumer,
                Runnable completionCallback,
                Consumer<Throwable> errorCallback);
}
