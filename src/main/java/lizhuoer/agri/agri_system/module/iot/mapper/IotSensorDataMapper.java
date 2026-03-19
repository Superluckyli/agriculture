package lizhuoer.agri.agri_system.module.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lizhuoer.agri.agri_system.module.iot.domain.IotSensorData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface IotSensorDataMapper extends BaseMapper<IotSensorData> {

    @Select("SELECT MAX(reported_at) FROM iot_sensor_data WHERE device_id = #{deviceId} AND sensor_type = #{sensorType}")
    LocalDateTime selectLatestReportedAt(@Param("deviceId") Long deviceId, @Param("sensorType") String sensorType);
}
