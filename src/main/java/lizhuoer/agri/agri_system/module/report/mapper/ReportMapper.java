package lizhuoer.agri.agri_system.module.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    /**
     * 统计各种作物的种植面积/或批次数量（简单起见统计批次数量）
     * 返回 [{name: '小麦', value: 10}, {name: '玉米', value: 5}]
     */
    @Select("SELECT v.crop_name as name, COUNT(b.batch_id) as value " +
            "FROM crop_batch b " +
            "LEFT JOIN base_crop_variety v ON b.variety_id = v.variety_id " +
            "WHERE b.is_active = 1 " +
            "GROUP BY v.crop_name")
    List<Map<String, Object>> countBatchByCrop();

    /**
     * 统计近7天的农事任务完成情况
     * 返回 [{date: '2023-10-01', count: 5}]
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM agri_task " +
            "WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY date " +
            "ORDER BY date ASC")
    List<Map<String, Object>> countTaskLast7Days();

    /**
     * 统计各类型传感器的最新平均数值（用于概览）
     */
    @Select("SELECT sensor_type as name, AVG(value) as value " +
            "FROM iot_sensor_data " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
            "GROUP BY sensor_type")
    List<Map<String, Object>> avgSensorValueLastHour();
}
