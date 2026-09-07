package com.fitness.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应 { list, total, page, pageSize }
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> list;
    private long total;
    private long page;
    private long pageSize;

    public PageResult() {
    }

    public PageResult(List<T> list, long total, long page, long pageSize) {
        this.list = list;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }
}
