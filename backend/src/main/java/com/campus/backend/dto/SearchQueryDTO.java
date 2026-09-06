package com.campus.backend.dto;

import lombok.Data;

@Data
public class SearchQueryDTO {

    private String keyword;

    private String type;

    /** 排序方式: price_asc, price_desc, time_desc, hot */
    private String sortBy;

    private Integer page = 1;

    private Integer size = 20;

    public int getOffset() {
        int safePage = (page == null || page < 1) ? 1 : page;
        int safeSize = (size == null || size < 1) ? 20 : size;
        return safeSize * (safePage - 1);
    }
}
