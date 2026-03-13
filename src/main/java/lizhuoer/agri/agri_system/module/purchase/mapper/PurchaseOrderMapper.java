package lizhuoer.agri.agri_system.module.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.purchase.domain.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    @Update("UPDATE purchase_order SET status = #{toStatus}, confirmed_by = #{confirmedBy}, " +
            "updated_at = NOW(), version = version + 1 " +
            "WHERE id = #{id} AND status = #{fromStatus} AND version = #{version}")
    int casUpdateStatus(@Param("id") Long id,
                        @Param("fromStatus") String fromStatus,
                        @Param("toStatus") String toStatus,
                        @Param("confirmedBy") Long confirmedBy,
                        @Param("version") Integer version);
}
