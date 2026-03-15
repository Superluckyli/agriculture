package lizhuoer.agri.agri_system.module.system.service;

import lizhuoer.agri.agri_system.module.system.domain.SysAuditLog;
import lizhuoer.agri.agri_system.module.system.mapper.SysAuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务 — 异步写入，不阻塞业务请求
 */
@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final SysAuditLogMapper auditLogMapper;

    public AuditLogService(SysAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Async
    public void record(SysAuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }
}
