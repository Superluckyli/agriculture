package lizhuoer.agri.agri_system.module.crop.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作物品种库
 */
@Data
@TableName("base_crop_variety")
public class BaseCropVariety {
    @TableId(type = IdType.AUTO)
    private Long varietyId;

    private String cropName;
    private Integer growthCycleDays;
    private BigDecimal idealHumidityMin;
    private BigDecimal idealHumidityMax;
    private BigDecimal idealTempMin;
    private BigDecimal idealTempMax;
    private LocalDateTime createTime;
}
