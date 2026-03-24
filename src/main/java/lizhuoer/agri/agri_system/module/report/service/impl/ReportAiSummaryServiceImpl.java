package lizhuoer.agri.agri_system.module.report.service.impl;

import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;
import lizhuoer.agri.agri_system.module.report.domain.ReportAiSummaryRequestDTO;
import lizhuoer.agri.agri_system.module.report.service.IReportAiSummaryService;
import lizhuoer.agri.agri_system.module.report.service.support.AiModelClient;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAiEvidenceBuilder;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAiPromptBuilder;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAiSectionStreamParser;
import lizhuoer.agri.agri_system.module.report.service.support.ReportAnalyticsContextBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@ConditionalOnBean(AiModelClient.class)
public class ReportAiSummaryServiceImpl implements IReportAiSummaryService {

    private final AiModelClient aiModelClient;
    private final ReportAnalyticsContextBuilder contextBuilder;
    private final ReportAiPromptBuilder promptBuilder;
    private final ReportAiEvidenceBuilder evidenceBuilder;

    public ReportAiSummaryServiceImpl(AiModelClient aiModelClient,
                                      ReportAnalyticsContextBuilder contextBuilder,
                                      ReportAiPromptBuilder promptBuilder,
                                      ReportAiEvidenceBuilder evidenceBuilder) {
        this.aiModelClient = aiModelClient;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.evidenceBuilder = evidenceBuilder;
    }

    @Override
    public void stream(ReportAiSummaryRequestDTO request,
                       Consumer<ReportAiStreamEventVO> eventConsumer,
                       Runnable completionCallback,
                       Consumer<Throwable> errorCallback) {
        // 统一管理“正常完成 / 异常结束 / 中途竞争”三类终止路径，
        // 避免 provider 回调和本地异常同时触发多个终止信号。
        StreamTermination termination = new StreamTermination(errorCallback);
        try {
            // 1. 先把当前 tab 的结构化 analytics 数据取出来
            ReportAnalyticsContextBuilder.ReportAnalyticsContext context = contextBuilder.build(request);
            // 2. 再基于结构化数据拼装 prompt，避免模型直接依赖图表配置
            String prompt = promptBuilder.build(context);
            // 3. parser 只负责把 provider 文本切成 section 事件；
            //    evidence 仍然由代码侧按 section 补发，保证证据可控且稳定。
            SectionEvidenceRelay relay = new SectionEvidenceRelay(context.currentTab(), context.analytics(), eventConsumer);
            ReportAiSectionStreamParser parser = new ReportAiSectionStreamParser(relay::accept);

            aiModelClient.stream(
                    prompt,
                    delta -> {
                        // 只有在 OPEN 状态下才允许继续接收 provider 文本。
                        if (!termination.allowDelta()) {
                            return;
                        }
                        try {
                            parser.accept(delta);
                        } catch (Throwable throwable) {
                            // 如果事件下游或 parser 本身报错，也统一走失败终止。
                            termination.fail(throwable);
                        }
                    },
                    () -> terminateCompletion(termination, parser, completionCallback),
                    throwable -> termination.fail(throwable)
            );
        } catch (Throwable throwable) {
            termination.fail(throwable);
        }
    }

    private void terminateCompletion(StreamTermination termination,
                                     ReportAiSectionStreamParser parser,
                                     Runnable completionCallback) {
        if (!termination.beginCompletion()) {
            return;
        }
        try {
            // 先把 parser 内部剩余缓冲区刷出来，再执行完成回调，
            // 最后才显式发 done，保证 transport 在看到 done 之前不会被关闭。
            parser.finish();
            completionCallback.run();
            parser.emitDone();
            termination.markCompleted();
        } catch (Throwable throwable) {
            termination.failAfterCompletionFailure(throwable);
        }
    }

    private final class SectionEvidenceRelay {
        private final String currentTab;
        private final Object analytics;
        private final Consumer<ReportAiStreamEventVO> downstream;
        private String activeSection;

        private SectionEvidenceRelay(String currentTab, Object analytics, Consumer<ReportAiStreamEventVO> downstream) {
            this.currentTab = currentTab;
            this.analytics = analytics;
            this.downstream = downstream;
        }

        private void accept(ReportAiStreamEventVO event) {
            if ("section-start".equals(event.getType())) {
                // 新 section 开始前，先把上一段的证据补出去。
                emitEvidenceIfReady();
                activeSection = event.getSection();
                downstream.accept(event);
                return;
            }
            if ("section-chunk".equals(event.getType())) {
                downstream.accept(event);
                return;
            }
            if ("done".equals(event.getType())) {
                emitEvidenceIfReady();
            }
            downstream.accept(event);
        }

        private void emitEvidenceIfReady() {
            if (activeSection == null) {
                return;
            }
            // evidence 永远来自结构化 analytics，而不是模型自己“口述”出来的数字。
            ReportAiStreamEventVO evidenceEvent = new ReportAiStreamEventVO();
            evidenceEvent.setType("evidence");
            evidenceEvent.setSection(activeSection);
            evidenceEvent.setEvidence(evidenceBuilder.build(currentTab, activeSection, analytics));
            downstream.accept(evidenceEvent);
            activeSection = null;
        }
    }

    private static final class StreamTermination {
        private enum State { OPEN, COMPLETING, TERMINATED }

        private final Consumer<Throwable> errorCallback;
        private State state = State.OPEN;

        private StreamTermination(Consumer<Throwable> errorCallback) {
            this.errorCallback = errorCallback;
        }

        private synchronized boolean allowDelta() {
            return state == State.OPEN;
        }

        private synchronized boolean beginCompletion() {
            // 只有 OPEN -> COMPLETING 这一次转换允许成功，
            // 后续重复完成或错误竞争都会被挡住。
            if (state != State.OPEN) {
                return false;
            }
            state = State.COMPLETING;
            return true;
        }

        private synchronized void markCompleted() {
            state = State.TERMINATED;
        }

        private void failAfterCompletionFailure(Throwable throwable) {
            synchronized (this) {
                state = State.TERMINATED;
            }
            errorCallback.accept(throwable);
        }

        private void fail(Throwable throwable) {
            synchronized (this) {
                if (state != State.OPEN) {
                    return;
                }
                state = State.TERMINATED;
            }
            errorCallback.accept(throwable);
        }
    }
}
