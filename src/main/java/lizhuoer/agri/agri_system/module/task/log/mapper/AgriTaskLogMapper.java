package lizhuoer.agri.agri_system.module.task.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.task.log.domain.AgriTaskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgriTaskLogMapper extends BaseMapper<AgriTaskLog> {

    @Select("SELECT COUNT(1) FROM agri_task_log WHERE image_urls LIKE CONCAT('%', #{url}, '%')")
    long countByImageUrl(@Param("url") String url);
}
