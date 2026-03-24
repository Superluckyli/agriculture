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

import java.util.concurrent.atomic.AtomicBoolean;
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
        try {
            ReportAnalyticsContextBuilder.ReportAnalyticsContext context = contextBuilder.build(request);
            String prompt = promptBuilder.build(context);
            SectionEvidenceRelay relay = new SectionEvidenceRelay(context.currentTab(), context.analytics(), eventConsumer);
            ReportAiSectionStreamParser parser = new ReportAiSectionStreamParser(relay::accept);
            AtomicBoolean terminated = new AtomicBoolean(false);

            aiModelClient.stream(
                    prompt,
                    parser::accept,
                    () -> {
                        if (terminated.compareAndSet(false, true)) {
                            parser.finish();
                            completionCallback.run();
                        }
                    },
                    throwable -> {
                        if (terminated.compareAndSet(false, true)) {
                            errorCallback.accept(throwable);
                        }
                    }
            );
        } catch (Throwable throwable) {
            errorCallback.accept(throwable);
        }
    }

    private final class SectionEvidenceRelay {
        private final String currentTab;
        private final Object analytics;
        private final Consumer<ReportAiStreamEventVO> downstream;
        private String activeSection;
        private final StringBuilder sectionBuffer = new StringBuilder();

        private SectionEvidenceRelay(String currentTab, Object analytics, Consumer<ReportAiStreamEventVO> downstream) {
            this.currentTab = currentTab;
            this.analytics = analytics;
            this.downstream = downstream;
        }

        private void accept(ReportAiStreamEventVO event) {
            if ("section-start".equals(event.getType())) {
                emitEvidenceIfReady();
                activeSection = event.getSection();
                sectionBuffer.setLength(0);
                downstream.accept(event);
                return;
            }
            if ("section-chunk".equals(event.getType())) {
                if (event.getSummary() != null) {
                    sectionBuffer.append(event.getSummary());
                }
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
            sectionBuffer.setLength(0);
        }
    }
}
