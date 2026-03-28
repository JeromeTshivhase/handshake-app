package com.trustmarket.handshake.listing.controller;

import com.trustmarket.handshake.listing.entity.Category;
import com.trustmarket.handshake.listing.service.CategoryService;
import com.trustmarket.handshake.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<Category>> all() {
        return ApiResponse.ok(categoryService.listAll());
    }
}