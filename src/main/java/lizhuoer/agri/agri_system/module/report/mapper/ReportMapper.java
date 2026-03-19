package lizhuoer.agri.agri_system.module.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    @Select("SELECT v.crop_name as name, COUNT(b.id) as value " +
            "FROM agri_crop_batch b " +
            "LEFT JOIN base_crop_variety v ON b.variety_id = v.variety_id " +
            "WHERE b.status != 'archived' " +
            "GROUP BY v.crop_name")
    List<Map<String, Object>> countBatchByCrop();

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM agri_task " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY date " +
            "ORDER BY date ASC")
    List<Map<String, Object>> countTaskLast7Days();

    @Select("SELECT sensor_type as name, AVG(sensor_value) as value " +
            "FROM iot_sensor_data " +
            "WHERE quality_status = 'VALID' " +
            "AND reported_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
            "GROUP BY sensor_type")
    List<Map<String, Object>> avgSensorValueLastHour();

    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN status_v2 = 'completed' THEN 1 ELSE 0 END) as completed " +
            "FROM agri_task WHERE DATE(create_time) = CURDATE()")
    Map<String, Object> countTodayTaskCompletion();

    @Select("SELECT COALESCE(SUM(area), 0) as totalArea, " +
            "COALESCE(SUM(CASE WHEN status = 1 THEN area ELSE 0 END), 0) as activeArea " +
            "FROM agri_farmland WHERE status != 2")
    Map<String, Object> sumFarmlandStats();

    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM purchase_order " +
            "WHERE status != 'cancelled' " +
            "AND YEAR(created_at) = YEAR(CURDATE()) " +
            "AND MONTH(created_at) = MONTH(CURDATE())")
    BigDecimal sumMonthlySpending();

    @Select("SELECT r.rule_name, e.sensor_type, e.trigger_value as value, " +
            "r.task_priority as priority, e.triggered_at as create_time " +
            "FROM iot_warning_event e " +
            "JOIN agri_task_rule r ON e.rule_id = r.rule_id " +
            "WHERE e.triggered_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
            "ORDER BY e.triggered_at DESC, r.task_priority ASC " +
            "LIMIT 20")
    List<Map<String, Object>> listActiveAlerts();

    @Select("SELECT COUNT(*) FROM iot_warning_event " +
            "WHERE triggered_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)")
    int countActiveAlerts();
}
