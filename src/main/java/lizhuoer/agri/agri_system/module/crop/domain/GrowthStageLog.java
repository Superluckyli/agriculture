package lizhuoer.agri.agri_system.module.crop.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 生长阶段记录
 */
@Data
@TableName("growth_stage_log")
public class GrowthStageLog {
    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long batchId;
    private String stageName;
    private LocalDateTime logDate;
    private String imageUrl;
    private String description;
}
