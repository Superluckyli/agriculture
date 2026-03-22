package lizhuoer.agri.agri_system.module.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Select("""
            <script>
            SELECT
              COALESCE(SUM(CASE WHEN t.assignee_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS assignedCount,
              COALESCE(SUM(CASE WHEN t.status_v2 = 'completed' THEN 1 ELSE 0 END), 0) AS completedCount,
              COALESCE(SUM(CASE
                WHEN t.status_v2 = 'completed'
                 AND t.deadline_at IS NOT NULL
                 AND t.completed_at IS NOT NULL
                 AND t.completed_at &lt;= t.deadline_at
                THEN 1 ELSE 0 END), 0) AS onTimeCompletedCount,
              COALESCE(SUM(CASE
                WHEN t.deadline_at IS NOT NULL AND (
                  t.status_v2 = 'overdue'
                  OR (t.status_v2 = 'completed' AND t.completed_at IS NOT NULL AND t.completed_at &gt; t.deadline_at)
                  OR (t.status_v2 &lt;&gt; 'completed' AND t.deadline_at &lt; NOW())
                )
                THEN 1 ELSE 0 END), 0) AS overdueCount
            FROM agri_task t
            LEFT JOIN agri_crop_batch b ON b.id = t.batch_id
            WHERE t.create_time <![CDATA[>=]]> #{startAt}
              AND t.create_time <![CDATA[<]]> #{endAt}
              <if test="farmlandId != null">
                AND COALESCE(t.source_farmland_id, b.farmland_id) = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
              <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
              </if>
            </script>
            """)
    Map<String, Object> countAnalyticsTaskSummary(@Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt,
                                                  @Param("farmlandId") Long farmlandId,
                                                  @Param("varietyId") Long varietyId,
                                                  @Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT COALESCE(COUNT(*), 0) AS activeBatchCount
            FROM agri_crop_batch b
            WHERE b.status IN ('in_progress', 'not_started')
              <if test="farmlandId != null">
                AND b.farmland_id = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
            </script>
            """)
    Map<String, Object> countAnalyticsActiveBatchSummary(@Param("farmlandId") Long farmlandId,
                                                         @Param("varietyId") Long varietyId);

    @Select("""
            <script>
            SELECT
              COALESCE(SUM(CASE WHEN b.target_output IS NOT NULL AND b.target_output &gt; 0 THEN b.target_output ELSE 0 END), 0) AS targetTotal,
              COALESCE(SUM(CASE WHEN b.target_output IS NOT NULL AND b.target_output &gt; 0 THEN COALESCE(b.actual_output, 0) ELSE 0 END), 0) AS actualTotal
            FROM agri_crop_batch b
            WHERE b.status != 'archived'
              <if test="farmlandId != null">
                AND b.farmland_id = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
            </script>
            """)
    Map<String, Object> sumAnalyticsOutputSummary(@Param("farmlandId") Long farmlandId,
                                                  @Param("varietyId") Long varietyId);

    @Select("""
            <script>
            SELECT
              COALESCE(
                (
                  SELECT SUM(po.total_amount)
                  FROM purchase_order po
                  WHERE po.status != 'cancelled'
                    AND po.created_at <![CDATA[>=]]> #{startAt}
                    AND po.created_at <![CDATA[<]]> #{endAt}
                    <if test="supplierId != null">
                      AND po.supplier_id = #{supplierId}
                    </if>
                ),
                0
              ) AS purchaseAmount,
              COALESCE(
                (
                  SELECT SUM(msl.qty * COALESCE(mi.unit_price, 0))
                  FROM material_stock_log msl
                  JOIN material_info mi ON mi.material_id = msl.material_id
                  WHERE msl.change_type = 'OUT'
                    AND msl.created_at <![CDATA[>=]]> #{startAt}
                    AND msl.created_at <![CDATA[<]]> #{endAt}
                    <if test="materialCategory != null and materialCategory != ''">
                      AND mi.category = #{materialCategory}
                    </if>
                    <if test="supplierId != null">
                      AND mi.supplier_id = #{supplierId}
                    </if>
                ),
                0
              ) AS materialCost
            </script>
            """)
    Map<String, Object> sumAnalyticsCostSummary(@Param("startAt") LocalDateTime startAt,
                                                @Param("endAt") LocalDateTime endAt,
                                                @Param("materialCategory") String materialCategory,
                                                @Param("supplierId") Long supplierId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(t.create_time, #{bucketFormat}) AS label, COUNT(*) AS count
            FROM agri_task t
            LEFT JOIN agri_crop_batch b ON b.id = t.batch_id
            WHERE t.create_time <![CDATA[>=]]> #{startAt}
              AND t.create_time <![CDATA[<]]> #{endAt}
              <if test="farmlandId != null">
                AND COALESCE(t.source_farmland_id, b.farmland_id) = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
              <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
              </if>
            GROUP BY DATE_FORMAT(t.create_time, #{bucketFormat})
            ORDER BY label ASC
            </script>
            """)
    List<Map<String, Object>> countTaskCreatedTrend(@Param("startAt") LocalDateTime startAt,
                                                    @Param("endAt") LocalDateTime endAt,
                                                    @Param("bucketFormat") String bucketFormat,
                                                    @Param("farmlandId") Long farmlandId,
                                                    @Param("varietyId") Long varietyId,
                                                    @Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(t.completed_at, #{bucketFormat}) AS label, COUNT(*) AS count
            FROM agri_task t
            LEFT JOIN agri_crop_batch b ON b.id = t.batch_id
            WHERE t.status_v2 = 'completed'
              AND t.completed_at IS NOT NULL
              AND t.completed_at <![CDATA[>=]]> #{startAt}
              AND t.completed_at <![CDATA[<]]> #{endAt}
              <if test="farmlandId != null">
                AND COALESCE(t.source_farmland_id, b.farmland_id) = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
              <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
              </if>
            GROUP BY DATE_FORMAT(t.completed_at, #{bucketFormat})
            ORDER BY label ASC
            </script>
            """)
    List<Map<String, Object>> countTaskCompletedTrend(@Param("startAt") LocalDateTime startAt,
                                                      @Param("endAt") LocalDateTime endAt,
                                                      @Param("bucketFormat") String bucketFormat,
                                                      @Param("farmlandId") Long farmlandId,
                                                      @Param("varietyId") Long varietyId,
                                                      @Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT DATE_FORMAT(t.deadline_at, #{bucketFormat}) AS label, COUNT(*) AS count
            FROM agri_task t
            LEFT JOIN agri_crop_batch b ON b.id = t.batch_id
            WHERE t.deadline_at IS NOT NULL
              AND t.deadline_at <![CDATA[>=]]> #{startAt}
              AND t.deadline_at <![CDATA[<]]> #{endAt}
              AND (
                t.status_v2 = 'overdue'
                OR (t.status_v2 = 'completed' AND t.completed_at IS NOT NULL AND t.completed_at > t.deadline_at)
                OR (t.status_v2 &lt;&gt; 'completed' AND t.deadline_at &lt; NOW())
              )
              <if test="farmlandId != null">
                AND COALESCE(t.source_farmland_id, b.farmland_id) = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
              <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
              </if>
            GROUP BY DATE_FORMAT(t.deadline_at, #{bucketFormat})
            ORDER BY label ASC
            </script>
            """)
    List<Map<String, Object>> countTaskOverdueTrend(@Param("startAt") LocalDateTime startAt,
                                                    @Param("endAt") LocalDateTime endAt,
                                                    @Param("bucketFormat") String bucketFormat,
                                                    @Param("farmlandId") Long farmlandId,
                                                    @Param("varietyId") Long varietyId,
                                                    @Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT
              DATE_FORMAT(t.create_time, #{bucketFormat}) AS label,
              t.status_v2 AS status,
              COUNT(*) AS count
            FROM agri_task t
            LEFT JOIN agri_crop_batch b ON b.id = t.batch_id
            WHERE t.create_time <![CDATA[>=]]> #{startAt}
              AND t.create_time <![CDATA[<]]> #{endAt}
              <if test="farmlandId != null">
                AND COALESCE(t.source_farmland_id, b.farmland_id) = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
              <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
              </if>
            GROUP BY DATE_FORMAT(t.create_time, #{bucketFormat}), t.status_v2
            ORDER BY label ASC, status ASC
            </script>
            """)
    List<Map<String, Object>> countTaskStatusDistribution(@Param("startAt") LocalDateTime startAt,
                                                          @Param("endAt") LocalDateTime endAt,
                                                          @Param("bucketFormat") String bucketFormat,
                                                          @Param("farmlandId") Long farmlandId,
                                                          @Param("varietyId") Long varietyId,
                                                          @Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT
              t.assignee_id AS assigneeId,
              COALESCE(u.real_name, u.username, CONCAT('用户', t.assignee_id)) AS assigneeName,
              COUNT(*) AS assignedCount,
              SUM(CASE WHEN t.status_v2 = 'completed' THEN 1 ELSE 0 END) AS completedCount,
              SUM(CASE
                WHEN t.status_v2 = 'completed'
                 AND t.deadline_at IS NOT NULL
                 AND t.completed_at IS NOT NULL
                 AND t.completed_at &lt;= t.deadline_at
                THEN 1 ELSE 0 END) AS onTimeCompletedCount,
              SUM(CASE
                WHEN t.deadline_at IS NOT NULL AND (
                  t.status_v2 = 'overdue'
                  OR (t.status_v2 = 'completed' AND t.completed_at IS NOT NULL AND t.completed_at &gt; t.deadline_at)
                  OR (t.status_v2 &lt;&gt; 'completed' AND t.deadline_at &lt; NOW())
                )
                THEN 1 ELSE 0 END) AS overdueCount
            FROM agri_task t
            LEFT JOIN agri_crop_batch b ON b.id = t.batch_id
            LEFT JOIN sys_user u ON u.user_id = t.assignee_id
            WHERE t.assignee_id IS NOT NULL
              AND t.create_time <![CDATA[>=]]> #{startAt}
              AND t.create_time <![CDATA[<]]> #{endAt}
              <if test="farmlandId != null">
                AND COALESCE(t.source_farmland_id, b.farmland_id) = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
              <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
              </if>
            GROUP BY t.assignee_id, u.real_name, u.username
            ORDER BY assignedCount DESC, assigneeName ASC
            </script>
            """)
    List<Map<String, Object>> listTaskAssigneeRanking(@Param("startAt") LocalDateTime startAt,
                                                      @Param("endAt") LocalDateTime endAt,
                                                      @Param("farmlandId") Long farmlandId,
                                                      @Param("varietyId") Long varietyId,
                                                      @Param("assigneeId") Long assigneeId);

    @Select("""
            <script>
            SELECT
              t.task_id AS taskId,
              t.task_name AS taskName,
              COALESCE(u.real_name, u.username, CONCAT('用户', t.assignee_id)) AS assigneeName,
              t.status_v2 AS statusV2,
              DATE_FORMAT(t.deadline_at, '%Y-%m-%d %H:%i:%s') AS deadlineAt,
              t.risk_level AS riskLevel,
              CASE
                WHEN t.deadline_at IS NULL THEN 0
                ELSE GREATEST(TIMESTAMPDIFF(DAY, t.deadline_at, COALESCE(t.completed_at, NOW())), 0)
              END AS overdueDays
            FROM agri_task t
            LEFT JOIN agri_crop_batch b ON b.id = t.batch_id
            LEFT JOIN sys_user u ON u.user_id = t.assignee_id
            WHERE t.create_time <![CDATA[>=]]> #{startAt}
              AND t.create_time <![CDATA[<]]> #{endAt}
              AND (
                t.status_v2 = 'overdue'
                OR t.status_v2 IN ('rejected_reassign', 'rejected_review', 'suspended')
                OR (t.deadline_at IS NOT NULL AND t.status_v2 &lt;&gt; 'completed' AND t.deadline_at &lt; NOW())
                OR (t.status_v2 = 'completed' AND t.deadline_at IS NOT NULL AND t.completed_at IS NOT NULL AND t.completed_at &gt; t.deadline_at)
              )
              <if test="farmlandId != null">
                AND COALESCE(t.source_farmland_id, b.farmland_id) = #{farmlandId}
              </if>
              <if test="varietyId != null">
                AND b.variety_id = #{varietyId}
              </if>
              <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
              </if>
            ORDER BY overdueDays DESC, t.deadline_at ASC
            LIMIT 20
            </script>
            """)
    List<Map<String, Object>> listAbnormalTasks(@Param("startAt") LocalDateTime startAt,
                                                @Param("endAt") LocalDateTime endAt,
                                                @Param("farmlandId") Long farmlandId,
                                                @Param("varietyId") Long varietyId,
                                                @Param("assigneeId") Long assigneeId);
}
