package lizhuoer.agri.agri_system.module.crop.farmland.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("agri_farmland")
public class AgriFarmland {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long orgId;
    @NotBlank(message = "农田名称不能为空")
    private String name;
    private String code;
    private String location;
    @Positive(message = "面积必须大于0")
    private BigDecimal area;
    private Long managerUserId;
    private String cropAdaptNote;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
