package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统一搜索接口
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public Result<Map<String, Object>> search(@RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(searchService.search(keyword, limit));
    }
}
