package com.pixfactory.web;

import com.pixfactory.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/busca")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public Map<String, Object> search(@RequestParam(defaultValue = "") String q) {
        return searchService.search(q);
    }
}
