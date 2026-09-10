package com.customerservice.controller;

import com.customerservice.common.Result;
import com.customerservice.entity.KnowledgeCategory;
import com.customerservice.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final KnowledgeService knowledgeService;

    @GetMapping
    public Result<List<KnowledgeCategory>> list() {
        return Result.ok(knowledgeService.listCategories());
    }

    @PostMapping
    public Result<Void> create(@RequestBody KnowledgeCategory category) {
        knowledgeService.createCategory(category);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody KnowledgeCategory category) {
        knowledgeService.updateCategory(id, category);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.deleteCategory(id);
        return Result.ok();
    }
}
