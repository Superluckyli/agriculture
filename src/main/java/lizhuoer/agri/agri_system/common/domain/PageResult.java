package lizhuoer.agri.agri_system.common.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应 VO — 替代直接暴露 MyBatis-Plus Page 对象
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 数据列表 */
    private List<T> items;

    /** 当前页码 (从 1 开始) */
    private long page;

    /** 每页大小 */
    private long size;

    /** 总记录数 */
    private long total;

    /**
     * 从 MyBatis-Plus Page 对象转换
     */
    public static <T> PageResult<T> from(Page<T> mpPage) {
        PageResult<T> result = new PageResult<>();
        result.setItems(mpPage.getRecords() != null ? mpPage.getRecords() : Collections.emptyList());
        result.setPage(mpPage.getCurrent());
        result.setSize(mpPage.getSize());
        result.setTotal(mpPage.getTotal());
        return result;
    }
}
