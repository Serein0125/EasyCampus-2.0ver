package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.dto.SearchQueryDTO;
import com.campus.backend.dto.SearchResultVO;
import com.campus.backend.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v2/search")
@Tag(name = "全局搜索", description = "跨类型关键词搜索：帖子、商品、用户")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "全局搜索", description = "根据关键词搜索帖子(标题/内容/作者)、商品(名称/描述/卖家)、用户(用户名/昵称)")
    public Result<SearchResultVO> search(SearchQueryDTO query) {
        if (query.getKeyword() == null || query.getKeyword().isBlank()) {
            return Result.error("搜索关键词不能为空");
        }
        query.setKeyword(query.getKeyword().trim());
        log.info("全局搜索: keyword={}, type={}, page={}, size={}", query.getKeyword(), query.getType(), query.getPage(), query.getSize());
        SearchResultVO result = searchService.search(query);
        return Result.success(result);
    }
}
