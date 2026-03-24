package lizhuoer.agri.agri_system.module.report.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReportAiSummaryRequestDTO {

    @NotBlank
    @Pattern(regexp = "task|production|cost", message = "currentTab must be one of: task, production, cost")
    private String currentTab;

    @NotNull
    @Valid
    private ReportAnalyticsFilterDTO filters;
}
