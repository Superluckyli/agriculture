package lizhuoer.agri.agri_system.module.report.domain;

import lombok.Data;

import java.util.List;

@Data
public class ReportAiStreamEventVO {
    private String type;
    private String section;
    private String summary;
    private List<ReportAiEvidenceItemVO> evidence;
}
