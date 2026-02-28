package lizhuoer.agri.agri_system.module.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface AgriTaskMapper extends BaseMapper<AgriTask> {
    @Update("""
            UPDATE agri_task
            SET assignee_id = #{executorId},
                executor_id = #{executorId},
                status = #{toStatus},
                assign_time = #{assignTime},
                assign_by = #{operatorId},
                assign_remark = #{remark},
                accept_time = NULL,
                accept_by = NULL,
                reject_time = NULL,
                reject_by = NULL,
                reject_reason = NULL,
                update_time = #{updateTime},
                update_by = #{operatorId},
                version = version + 1
            WHERE task_id = #{taskId}
              AND status = #{fromStatus}
              AND version = #{version}
            """)
    int assignTask(@Param("taskId") Long taskId,
            @Param("executorId") Long executorId,
            @Param("operatorId") Long operatorId,
            @Param("remark") String remark,
            @Param("assignTime") LocalDateTime assignTime,
            @Param("updateTime") LocalDateTime updateTime,
            @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus,
            @Param("version") Integer version);

    @Update("""
            UPDATE agri_task
            SET status = #{toStatus},
                accept_time = #{acceptTime},
                accept_by = #{operatorId},
                update_time = #{updateTime},
                update_by = #{operatorId},
                version = version + 1
            WHERE task_id = #{taskId}
              AND assignee_id = #{assigneeId}
              AND status = #{fromStatus}
              AND version = #{version}
            """)
    int acceptTask(@Param("taskId") Long taskId,
            @Param("assigneeId") Long assigneeId,
            @Param("operatorId") Long operatorId,
            @Param("acceptTime") LocalDateTime acceptTime,
            @Param("updateTime") LocalDateTime updateTime,
            @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus,
            @Param("version") Integer version);

    @Update("""
            UPDATE agri_task
            SET status = #{toStatus},
                assignee_id = NULL,
                executor_id = NULL,
                accept_time = NULL,
                accept_by = NULL,
                reject_time = #{rejectTime},
                reject_by = #{operatorId},
                reject_reason = #{reason},
                update_time = #{updateTime},
                update_by = #{operatorId},
                version = version + 1
            WHERE task_id = #{taskId}
              AND assignee_id = #{assigneeId}
              AND status = #{fromStatus}
              AND version = #{version}
            """)
    int rejectTask(@Param("taskId") Long taskId,
            @Param("assigneeId") Long assigneeId,
            @Param("operatorId") Long operatorId,
            @Param("reason") String reason,
            @Param("rejectTime") LocalDateTime rejectTime,
            @Param("updateTime") LocalDateTime updateTime,
            @Param("fromStatus") Integer fromStatus,
            @Param("toStatus") Integer toStatus,
            @Param("version") Integer version);
}
