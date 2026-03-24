package lizhuoer.agri.agri_system.module.task.job;

import lizhuoer.agri.agri_system.module.task.log.service.IAgriTaskLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskLogImageCleanupSchedulerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-24T08:15:30Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Mock
    private IAgriTaskLogService taskLogService;

    private TaskLogImageCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TaskLogImageCleanupScheduler(taskLogService, tempDir, "/uploads", FIXED_CLOCK);
    }

    @Test
    void deletesOldFilesNotReferencedByAnyLog() throws Exception {
        Path orphan = createFile("task-log/2026/03/24/orphan.jpg", Duration.ofHours(30));
        when(taskLogService.isImageUrlReferenced("/uploads/task-log/2026/03/24/orphan.jpg")).thenReturn(false);

        scheduler.cleanupOrphanImages();

        assertThat(Files.exists(orphan)).isFalse();
        verify(taskLogService).isImageUrlReferenced("/uploads/task-log/2026/03/24/orphan.jpg");
    }

    @Test
    void keepsOldFilesThatAreStillReferenced() throws Exception {
        Path used = createFile("task-log/2026/03/24/used.jpg", Duration.ofHours(30));
        when(taskLogService.isImageUrlReferenced("/uploads/task-log/2026/03/24/used.jpg")).thenReturn(true);

        scheduler.cleanupOrphanImages();

        assertThat(Files.exists(used)).isTrue();
        verify(taskLogService).isImageUrlReferenced("/uploads/task-log/2026/03/24/used.jpg");
    }

    @Test
    void ignoresFilesNewerThanTwentyFourHours() throws Exception {
        Path recent = createFile("task-log/2026/03/24/recent.jpg", Duration.ofHours(2));

        scheduler.cleanupOrphanImages();

        assertThat(Files.exists(recent)).isTrue();
        verify(taskLogService, never()).isImageUrlReferenced("/uploads/task-log/2026/03/24/recent.jpg");
    }

    private Path createFile(String relativePath, Duration age) throws Exception {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{1, 2, 3});
        Files.setLastModifiedTime(file, FileTime.from(Instant.now(FIXED_CLOCK).minus(age)));
        return file;
    }
}
