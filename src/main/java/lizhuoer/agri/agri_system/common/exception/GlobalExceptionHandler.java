package lizhuoer.agri.agri_system.common.exception;

import lizhuoer.agri.agri_system.common.domain.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 统一错误码 + HTTP 状态码对齐
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("请求地址'{}', 参数校验失败: {}", request.getRequestURI(), msg);
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e, HttpServletRequest request) {
        String msg = e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("请求地址'{}', 绑定失败: {}", request.getRequestURI(), msg);
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("请求地址'{}', 参数错误.", request.getRequestURI(), e);
        return R.fail(ErrorCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 业务异常 — 使用 ErrorCode 中定义的码值
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("请求地址'{}', 业务异常: {}", request.getRequestURI(), e.getMessage());
        return R.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntime(RuntimeException e, HttpServletRequest request) {
        log.error("请求地址'{}', 发生未知异常.", request.getRequestURI(), e);
        return R.fail(ErrorCode.INTERNAL_ERROR.getCode(), "系统异常，请稍后重试");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("请求地址'{}', 发生系统异常.", request.getRequestURI(), e);
        return R.fail(ErrorCode.INTERNAL_ERROR.getCode(), "系统异常，请稍后重试");
    }
}
