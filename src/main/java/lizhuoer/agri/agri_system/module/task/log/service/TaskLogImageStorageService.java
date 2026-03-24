package lizhuoer.agri.agri_system.module.task.log.service;

import lizhuoer.agri.agri_system.module.task.log.domain.TaskLogImageUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface TaskLogImageStorageService {

    TaskLogImageUploadVO store(MultipartFile file);
}
