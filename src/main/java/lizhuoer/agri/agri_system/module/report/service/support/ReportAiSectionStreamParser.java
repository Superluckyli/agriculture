package lizhuoer.agri.agri_system.module.report.service.support;

import lizhuoer.agri.agri_system.module.report.domain.ReportAiStreamEventVO;

import java.util.Set;
import java.util.function.Consumer;

public class ReportAiSectionStreamParser {

    private static final String MARKER_PREFIX = "[SECTION:";
    private static final Set<String> SUPPORTED_SECTIONS = Set.of("conclusion", "reason", "risk", "attention");

    private final Consumer<ReportAiStreamEventVO> eventConsumer;
    private final StringBuilder buffer = new StringBuilder();
    private String activeSection;

    public ReportAiSectionStreamParser(Consumer<ReportAiStreamEventVO> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    public void accept(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        buffer.append(delta);
        drain(false);
    }

    public void finish() {
        drain(true);
        buffer.setLength(0);
    }

    public void emitDone() {
        ReportAiStreamEventVO done = new ReportAiStreamEventVO();
        done.setType("done");
        eventConsumer.accept(done);
    }

    private void drain(boolean finishing) {
        while (buffer.length() > 0) {
            int markerIndex = buffer.indexOf(MARKER_PREFIX);
            if (markerIndex < 0) {
                flushTrailingText(finishing);
                return;
            }

            if (markerIndex > 0) {
                emitChunk(buffer.substring(0, markerIndex));
                buffer.delete(0, markerIndex);
                continue;
            }

            int markerEnd = buffer.indexOf("]");
            if (markerEnd < 0) {
                if (finishing) {
                    buffer.setLength(0);
                }
                return;
            }

            String section = parseSection(buffer.substring(0, markerEnd + 1));
            buffer.delete(0, markerEnd + 1);
            if (section != null) {
                activeSection = section;
                ReportAiStreamEventVO event = new ReportAiStreamEventVO();
                event.setType("section-start");
                event.setSection(section);
                eventConsumer.accept(event);
            } else {
                activeSection = null;
            }
        }
    }

    private void flushTrailingText(boolean finishing) {
        if (buffer.length() == 0) {
            return;
        }

        int retain = finishing ? 0 : longestMarkerPrefixSuffix(buffer);
        int emitLength = buffer.length() - retain;
        if (emitLength > 0) {
            emitChunk(buffer.substring(0, emitLength));
            buffer.delete(0, emitLength);
        }
    }

    private int longestMarkerPrefixSuffix(CharSequence source) {
        int max = Math.min(source.length(), MARKER_PREFIX.length() - 1);
        for (int len = max; len > 0; len--) {
            if (endsWithPrefix(source, len)) {
                return len;
            }
        }
        return 0;
    }

    private boolean endsWithPrefix(CharSequence source, int len) {
        int start = source.length() - len;
        for (int i = 0; i < len; i++) {
            if (source.charAt(start + i) != MARKER_PREFIX.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private String parseSection(String marker) {
        if (!marker.startsWith(MARKER_PREFIX) || !marker.endsWith("]")) {
            return null;
        }
        String section = marker.substring(MARKER_PREFIX.length(), marker.length() - 1);
        return SUPPORTED_SECTIONS.contains(section) ? section : null;
    }

    private void emitChunk(String text) {
        if (activeSection == null || text == null || text.isEmpty()) {
            return;
        }
        ReportAiStreamEventVO event = new ReportAiStreamEventVO();
        event.setType("section-chunk");
        event.setSection(activeSection);
        event.setSummary(text);
        eventConsumer.accept(event);
    }
}
