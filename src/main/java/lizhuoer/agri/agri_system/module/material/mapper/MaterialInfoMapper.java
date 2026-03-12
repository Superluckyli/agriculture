package lizhuoer.agri.agri_system.module.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.material.domain.MaterialInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MaterialInfoMapper extends BaseMapper<MaterialInfo> {
    @Update("""
            UPDATE material_info
            SET current_stock = current_stock - #{qty},
                updated_at = NOW(),
                version = version + 1
            WHERE material_id = #{materialId}
              AND current_stock >= #{qty}
              AND version = #{version}
            """)
    int deductStock(@Param("materialId") Long materialId,
            @Param("qty") BigDecimal qty,
            @Param("version") Integer version);

    @Update("""
            UPDATE material_info
            SET current_stock = current_stock + #{qty},
                updated_at = NOW(),
                version = version + 1
            WHERE material_id = #{materialId}
              AND version = #{version}
            """)
    int addStock(@Param("materialId") Long materialId,
            @Param("qty") BigDecimal qty,
            @Param("version") Integer version);
}
