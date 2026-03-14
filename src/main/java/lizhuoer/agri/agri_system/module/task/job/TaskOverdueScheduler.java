package lizhuoer.agri.agri_system.module.task.job;

import lizhuoer.agri.agri_system.module.task.service.IAgriTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时扫描逾期任务，5 分钟一次
 */
@Component
public class TaskOverdueScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskOverdueScheduler.class);

    private final IAgriTaskService taskService;

    public TaskOverdueScheduler(IAgriTaskService taskService) {
        this.taskService = taskService;
    }

    @Scheduled(fixedDelay = 300_000)
    public void scanAndMarkOverdue() {
        int count = taskService.markOverdueTasks(LocalDateTime.now());
        if (count > 0) {
            log.info("Marked {} tasks as overdue", count);
        }
    }
}
