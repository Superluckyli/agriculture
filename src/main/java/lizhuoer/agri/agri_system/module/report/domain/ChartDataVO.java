package lizhuoer.agri.agri_system.module.report.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ChartDataVO {
    private List<String> xAxis;
    private List<Object> series;
    private String title;

    public ChartDataVO(List<String> xAxis, List<Object> series) {
        this.xAxis = xAxis;
        this.series = series;
    }
}
