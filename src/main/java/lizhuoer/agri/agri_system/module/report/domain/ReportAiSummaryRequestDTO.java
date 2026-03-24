package lizhuoer.agri.agri_system.module.report.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportAiSummaryRequestDTO {

    @NotBlank
    private String currentTab;

    @NotNull
    @Valid
    private ReportAnalyticsFilterDTO filters;
}
