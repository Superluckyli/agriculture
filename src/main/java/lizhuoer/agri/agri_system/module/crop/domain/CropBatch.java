package lizhuoer.agri.agri_system.module.crop.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 种植批次
 */
@Data
@TableName("crop_batch")
public class CropBatch {
    @TableId(type = IdType.AUTO)
    private Long batchId;

    private Long varietyId;
    private String plotId;
    private LocalDate sowingDate;
    private LocalDate expectedHarvestDate;
    private String currentStage;
    private Integer isActive;

    @TableField(exist = false)
    private String cropName; // 关联查询用
}
