package lizhuoer.agri.agri_system.module.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.task.domain.AgriTask;
import lizhuoer.agri.agri_system.module.task.mapper.AgriTaskMapper;
import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AgriTaskServiceImpl extends ServiceImpl<AgriTaskMapper, AgriTask> implements IAgriTaskService {

    @Override
    public void createAutoTask(AgriTask task) {
        if (task.getCreateTime() == null) {
            task.setCreateTime(LocalDateTime.now());
        }
        // 自动生成的任务状态一般为 0待分配
        task.setStatus(0);
        this.save(task);
        System.out.println("【任务中心】已自动生成任务: " + task.getTaskName());
    }
}
