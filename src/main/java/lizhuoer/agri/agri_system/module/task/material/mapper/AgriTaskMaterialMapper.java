package lizhuoer.agri.agri_system.module.task.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.task.material.domain.AgriTaskMaterial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgriTaskMaterialMapper extends BaseMapper<AgriTaskMaterial> {

    @Select("""
            SELECT tm.*, mi.name AS material_name
            FROM agri_task_material tm
            LEFT JOIN material_info mi ON tm.material_id = mi.material_id
            WHERE tm.task_id = #{taskId}
            ORDER BY tm.id
            """)
    List<AgriTaskMaterial> listWithNameByTaskId(@Param("taskId") Long taskId);
}
