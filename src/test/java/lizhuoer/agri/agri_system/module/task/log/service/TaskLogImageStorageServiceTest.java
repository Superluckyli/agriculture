package lizhuoer.agri.agri_system.module.task.log.service;

import lizhuoer.agri.agri_system.module.task.log.domain.TaskLogImageUploadVO;
import lizhuoer.agri.agri_system.module.task.log.service.impl.TaskLogImageStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskLogImageStorageServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-24T08:15:30Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private TaskLogImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new TaskLogImageStorageServiceImpl(tempDir, "/uploads", FIXED_CLOCK);
    }

    @ParameterizedTest
    @MethodSource("allowedImageFiles")
    void storesAllowedImageTypes(String fileName, String contentType) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType, new byte[128]);

        TaskLogImageUploadVO result = service.store(file);

        assertThat(result.getName()).isEqualTo(fileName);
        assertThat(result.getUrl()).startsWith("/uploads/task-log/2026/03/24/");
        assertThat(result.getUrl()).endsWith(fileName.substring(fileName.lastIndexOf('.')));

        Path storedFile = tempDir.resolve(result.getUrl().replaceFirst("^/uploads/", ""));
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.size(storedFile)).isEqualTo(128);
    }

    @Test
    void rejectsFileLargerThanFiveMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oversize.jpg",
                "image/jpeg",
                new byte[5 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    void rejectsUnsupportedFileExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.gif", "image/gif", new byte[32]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅支持");
    }

    @Test
    void rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "text/plain", new byte[32]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅支持");
    }

    @Test
    void rejectsMissingContentTypeEvenWhenExtensionLooksValid() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", null, new byte[32]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("仅支持");
    }

    @Test
    void generatesDatePartitionedUrlAndUuidFileName() {
        MockMultipartFile file = new MockMultipartFile("file", "field-photo.png", "image/png", new byte[32]);

        TaskLogImageUploadVO result = service.store(file);

        assertThat(result.getUrl())
                .matches("^/uploads/task-log/2026/03/24/[0-9a-f\\-]{36}\\.png$");
    }

    private static Stream<Arguments> allowedImageFiles() {
        return Stream.of(
                Arguments.of("photo.jpg", "image/jpeg"),
                Arguments.of("photo.jpeg", "image/jpeg"),
                Arguments.of("photo.png", "image/png"),
                Arguments.of("photo.webp", "image/webp")
        );
    }
}
