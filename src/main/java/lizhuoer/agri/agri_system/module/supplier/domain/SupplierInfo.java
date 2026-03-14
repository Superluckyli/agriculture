package lizhuoer.agri.agri_system.module.supplier.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("supplier_info")
public class SupplierInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    @NotBlank(message = "供应商名称不能为空")
    private String name;
    private String contactName;
    private String phone;
    private String address;
    private String remark;
    private Integer status;
    private LocalDateTime createdAt;
}
