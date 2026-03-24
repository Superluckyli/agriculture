package lizhuoer.agri.agri_system.module.task.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lizhuoer.agri.agri_system.module.system.domain.SysUser;
import lizhuoer.agri.agri_system.module.system.mapper.SysUserMapper;
import lizhuoer.agri.agri_system.module.task.log.domain.AgriTaskLog;
import lizhuoer.agri.agri_system.module.task.log.mapper.AgriTaskLogMapper;
import lizhuoer.agri.agri_system.module.task.log.service.IAgriTaskLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgriTaskLogServiceImpl extends ServiceImpl<AgriTaskLogMapper, AgriTaskLog> implements IAgriTaskLogService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public void addLog(Long taskId, String action, Long operatorId, String content) {
        AgriTaskLog log = new AgriTaskLog();
        log.setTaskId(taskId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setRemark(content);
        log.setCreatedAt(LocalDateTime.now());
        save(log);
    }

    @Override
    public List<AgriTaskLog> listByTaskId(Long taskId) {
        LambdaQueryWrapper<AgriTaskLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgriTaskLog::getTaskId, taskId)
                .orderByDesc(AgriTaskLog::getCreatedAt);
        List<AgriTaskLog> logs = list(wrapper);

        // 批量填充 operatorName
        Set<Long> operatorIds = logs.stream()
                .map(AgriTaskLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!operatorIds.isEmpty()) {
            Map<Long, String> nameMap = sysUserMapper.selectBatchIds(operatorIds).stream()
                    .collect(Collectors.toMap(SysUser::getUserId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
            logs.forEach(l -> {
                if (l.getOperatorId() != null) {
                    l.setOperatorName(nameMap.get(l.getOperatorId()));
                }
            });
        }
        return logs;
    }

    @Override
    public boolean isImageUrlReferenced(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        return baseMapper.countByImageUrl(url) > 0;
    }
}
