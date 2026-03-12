package lizhuoer.agri.agri_system.module.crop.batch.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("agri_crop_batch")
public class AgriCropBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long orgId;
    private String batchNo;
    private Long farmlandId;
    private Long varietyId;
    private String cropVariety;
    private LocalDate plantingDate;
    private LocalDate estimatedHarvestDate;
    private LocalDate actualHarvestDate;
    private String stage;
    private String status;
    private Long ownerUserId;
    private BigDecimal targetOutput;
    private BigDecimal actualOutput;
    private String abandonReason;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String farmlandName;

    @TableField(exist = false)
    private String varietyName;
}
