package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    /**
     * 新增分类
     * @param category
     */
    @AutoFill(OperationType.INSERT)
    void insert(Category category);

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 修改分类
     * @param category
     */
    @AutoFill(OperationType.UPDATE)
    void update(Category category);

    /**
     * 根据id删除分类
     * @param id
     */
    @Delete("delete from category where id=#{id}")
    void delete(Long id);

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    List<Category> selectByType(Integer type);
}
