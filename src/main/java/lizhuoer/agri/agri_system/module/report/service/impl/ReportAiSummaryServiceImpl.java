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
        StreamTermination termination = new StreamTermination(errorCallback);
        try {
            ReportAnalyticsContextBuilder.ReportAnalyticsContext context = contextBuilder.build(request);
            String prompt = promptBuilder.build(context);
            SectionEvidenceRelay relay = new SectionEvidenceRelay(context.currentTab(), context.analytics(), eventConsumer);
            ReportAiSectionStreamParser parser = new ReportAiSectionStreamParser(relay::accept);

            aiModelClient.stream(
                    prompt,
                    delta -> {
                        if (!termination.allowDelta()) {
                            return;
                        }
                        try {
                            parser.accept(delta);
                        } catch (Throwable throwable) {
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
