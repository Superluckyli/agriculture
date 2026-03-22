package lizhuoer.agri.agri_system.module.report.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class ReportAnalyticsFilterDTO {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String granularity;
    private Long farmlandId;
    private Long varietyId;
    private Long assigneeId;
    private String materialCategory;
    private Long supplierId;
}
