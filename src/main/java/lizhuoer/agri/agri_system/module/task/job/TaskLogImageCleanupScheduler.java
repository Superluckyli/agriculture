package lizhuoer.agri.agri_system.module.task.job;

import lizhuoer.agri.agri_system.module.task.log.service.IAgriTaskLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@Component
public class TaskLogImageCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(TaskLogImageCleanupScheduler.class);
    private static final Duration RETENTION = Duration.ofHours(24);

    private final IAgriTaskLogService taskLogService;
    private final Path taskLogRoot;
    private final String urlPrefix;
    private final Clock clock;

    @Autowired
    public TaskLogImageCleanupScheduler(
            IAgriTaskLogService taskLogService,
            @Value("${task.log.image.storage-root:uploads}") String storageRoot,
            @Value("${task.log.image.url-prefix:/uploads}") String urlPrefix) {
        this(taskLogService, Path.of(storageRoot), urlPrefix, Clock.systemDefaultZone());
    }

    TaskLogImageCleanupScheduler(IAgriTaskLogService taskLogService, Path storageRoot, String urlPrefix, Clock clock) {
        this.taskLogService = taskLogService;
        this.taskLogRoot = storageRoot.resolve("task-log").toAbsolutePath().normalize();
        this.urlPrefix = normalizeUrlPrefix(urlPrefix);
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanImages() {
        if (!Files.isDirectory(taskLogRoot)) {
            return;
        }

        Instant cutoff = Instant.now(clock).minus(RETENTION);
        int deletedCount = 0;

        try (Stream<Path> paths = Files.walk(taskLogRoot)) {
            for (Path file : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
                try {
                    if (!Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                        continue;
                    }

                    String publicUrl = toPublicUrl(file);
                    if (taskLogService.isImageUrlReferenced(publicUrl)) {
                        continue;
                    }

                    Files.deleteIfExists(file);
                    deletedCount++;
                } catch (Exception ex) {
                    log.warn("Failed to evaluate task log image cleanup for file {}", file, ex);
                }
            }
        } catch (IOException ex) {
            log.warn("Failed to scan task log image directory {}", taskLogRoot, ex);
            return;
        }

        if (deletedCount > 0) {
            log.info("Deleted {} orphan task log images", deletedCount);
        }
    }

    private String toPublicUrl(Path file) {
        String relativePath = taskLogRoot.getParent()
                .relativize(file.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
        return urlPrefix + "/" + relativePath;
    }

    private String normalizeUrlPrefix(String urlPrefix) {
        if (!StringUtils.hasText(urlPrefix)) {
            return "/uploads";
        }
        String normalized = urlPrefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
