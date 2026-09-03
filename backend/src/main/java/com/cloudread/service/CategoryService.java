package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudread.common.BusinessException;
import com.cloudread.common.Result;
import com.cloudread.dto.CategoryVO;
import com.cloudread.entity.Book;
import com.cloudread.entity.Category;
import com.cloudread.mapper.BookMapper;
import com.cloudread.mapper.CategoryMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final BookMapper bookMapper;

    public CategoryService(CategoryMapper categoryMapper, BookMapper bookMapper) {
        this.categoryMapper = categoryMapper;
        this.bookMapper = bookMapper;
    }

    @Cacheable(cacheNames = "categoryTree")
    public List<CategoryVO> tree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort).orderByAsc(Category::getId));
        Map<Long, CategoryVO> map = new LinkedHashMap<>();
        for (Category c : all) {
            CategoryVO vo = new CategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setParentId(c.getParentId());
            vo.setSort(c.getSort());
            map.put(c.getId(), vo);
        }
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : map.values()) {
            if (vo.getParentId() == null) {
                roots.add(vo);
            } else {
                CategoryVO parent = map.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    @CacheEvict(cacheNames = "categoryTree", allEntries = true)
    public Category create(String name, Long parentId, Integer sort) {
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setSort(sort == null ? 0 : sort);
        categoryMapper.insert(category);
        return category;
    }

    @CacheEvict(cacheNames = "categoryTree", allEntries = true)
    public Category update(Long id, String name, Long parentId, Integer sort) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "分类不存在");
        }
        if (id.equals(parentId)) {
            throw new BusinessException("父分类不能是自己");
        }
        category.setName(name);
        category.setParentId(parentId);
        category.setSort(sort == null ? 0 : sort);
        categoryMapper.updateById(category);
        return category;
    }

    @CacheEvict(cacheNames = "categoryTree", allEntries = true)
    public void delete(Long id) {
        Long children = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (children != null && children > 0) {
            throw new BusinessException("存在子分类，无法删除");
        }
        Long books = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, id));
        if (books != null && books > 0) {
            throw new BusinessException("该分类下存在书籍，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    /**
     * 返回该分类自身及所有子孙分类的 ID 集合（用于按一级分类查询其下全部书籍）。
     */
    public List<Long> categoryAndDescendantIds(Long categoryId) {
        List<Category> all = categoryMapper.selectList(null);
        Map<Long, List<Long>> children = new HashMap<>();
        for (Category c : all) {
            if (c.getParentId() != null) {
                children.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
            }
        }
        List<Long> result = new ArrayList<>();
        collect(categoryId, children, result);
        return result;
    }

    private void collect(Long id, Map<Long, List<Long>> children, List<Long> result) {
        if (id == null || result.contains(id)) {
            return;
        }
        result.add(id);
        List<Long> subs = children.get(id);
        if (subs != null) {
            for (Long sub : subs) {
                collect(sub, children, result);
            }
        }
    }
}
