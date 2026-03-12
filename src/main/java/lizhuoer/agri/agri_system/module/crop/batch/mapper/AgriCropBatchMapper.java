package lizhuoer.agri.agri_system.module.crop.batch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lizhuoer.agri.agri_system.module.crop.batch.domain.AgriCropBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgriCropBatchMapper extends BaseMapper<AgriCropBatch> {

    @Select("<script>" +
            "SELECT b.*, f.name AS farmland_name, v.crop_name AS variety_name " +
            "FROM agri_crop_batch b " +
            "LEFT JOIN agri_farmland f ON b.farmland_id = f.id " +
            "LEFT JOIN base_crop_variety v ON b.variety_id = v.variety_id " +
            "<where>" +
            "  <if test='batchNo != null and batchNo != \"\"'> AND b.batch_no LIKE CONCAT('%', #{batchNo}, '%')</if>" +
            "  <if test='status != null and status != \"\"'> AND b.status = #{status}</if>" +
            "  <if test='farmlandId != null'> AND b.farmland_id = #{farmlandId}</if>" +
            "</where>" +
            " ORDER BY b.id DESC" +
            "</script>")
    Page<AgriCropBatch> selectPageWithNames(Page<AgriCropBatch> page,
                                            @Param("batchNo") String batchNo,
                                            @Param("status") String status,
                                            @Param("farmlandId") Long farmlandId);
}
