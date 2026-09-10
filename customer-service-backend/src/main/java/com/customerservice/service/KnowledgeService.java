package com.customerservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.customerservice.common.BusinessException;
import com.customerservice.entity.Knowledge;
import com.customerservice.entity.KnowledgeCategory;
import com.customerservice.mapper.KnowledgeCategoryMapper;
import com.customerservice.mapper.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeCategoryMapper categoryMapper;

    public IPage<Knowledge> page(long page, long size, String keyword, Long categoryId, Integer status) {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Knowledge::getQuestion, keyword)
                    .or().like(Knowledge::getKeywords, keyword)
                    .or().like(Knowledge::getAnswer, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Knowledge::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Knowledge::getStatus, status);
        }
        wrapper.orderByDesc(Knowledge::getUpdatedAt);

        IPage<Knowledge> result = knowledgeMapper.selectPage(new Page<>(page, size), wrapper);
        fillCategoryName(result.getRecords());
        return result;
    }

    private void fillCategoryName(List<Knowledge> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(Knowledge::getCategoryId).filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getName));
        records.forEach(k -> k.setCategoryName(nameMap.get(k.getCategoryId())));
    }

    public void create(Knowledge knowledge) {
        validate(knowledge);
        if (knowledge.getHits() == null) {
            knowledge.setHits(0);
        }
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(1);
        }
        knowledge.setId(null);
        knowledgeMapper.insert(knowledge);
    }

    public void update(Long id, Knowledge knowledge) {
        validate(knowledge);
        knowledge.setId(id);
        knowledgeMapper.updateById(knowledge);
    }

    public void delete(Long id) {
        knowledgeMapper.deleteById(id);
    }

    private void validate(Knowledge knowledge) {
        if (!StringUtils.hasText(knowledge.getQuestion())) {
            throw new BusinessException("问题不能为空");
        }
        if (!StringUtils.hasText(knowledge.getAnswer())) {
            throw new BusinessException("答案不能为空");
        }
    }

    // ---------------- 分类 ----------------

    public List<KnowledgeCategory> listCategories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCategory>().orderByAsc(KnowledgeCategory::getSort).orderByAsc(KnowledgeCategory::getId));
    }

    public void createCategory(KnowledgeCategory category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        category.setId(null);
        categoryMapper.insert(category);
    }

    public void updateCategory(Long id, KnowledgeCategory category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        category.setId(id);
        categoryMapper.updateById(category);
    }

    public void deleteCategory(Long id) {
        Long count = knowledgeMapper.selectCount(new LambdaQueryWrapper<Knowledge>().eq(Knowledge::getCategoryId, id));
        if (count != null && count > 0) {
            throw new BusinessException("该分类下存在知识，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
