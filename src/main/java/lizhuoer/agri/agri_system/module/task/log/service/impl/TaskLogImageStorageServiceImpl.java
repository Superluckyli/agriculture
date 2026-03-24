package lizhuoer.agri.agri_system.module.task.log.service.impl;

import lizhuoer.agri.agri_system.module.task.log.domain.TaskLogImageUploadVO;
import lizhuoer.agri.agri_system.module.task.log.service.TaskLogImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskLogImageStorageServiceImpl implements TaskLogImageStorageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Path storageRoot;
    private final String urlPrefix;
    private final Clock clock;

    @Autowired
    public TaskLogImageStorageServiceImpl(
            @Value("${task.log.image.storage-root:uploads}") String storageRoot,
            @Value("${task.log.image.url-prefix:/uploads}") String urlPrefix) {
        this(Path.of(storageRoot), urlPrefix, Clock.systemDefaultZone());
    }

    public TaskLogImageStorageServiceImpl(Path storageRoot, String urlPrefix, Clock clock) {
        this.storageRoot = storageRoot;
        this.urlPrefix = normalizeUrlPrefix(urlPrefix);
        this.clock = clock;
    }

    @Override
    public TaskLogImageUploadVO store(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getValidatedExtension(originalFilename);
        LocalDate today = LocalDate.now(clock);
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path relativePath = Path.of(
                "task-log",
                String.format("%04d", today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                storedFileName
        );

        Path targetPath = storageRoot.resolve(relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("图片保存失败", e);
        }

        String url = urlPrefix + "/" + relativePath.toString().replace('\\', '/');
        return new TaskLogImageUploadVO(url, originalFilename);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("图片文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过5MB");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp 图片");
        }

        getValidatedExtension(file.getOriginalFilename());
    }

    private String getValidatedExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("图片文件名不能为空");
        }
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp 图片");
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、webp 图片");
        }
        return normalized;
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
