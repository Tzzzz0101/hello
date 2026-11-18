package com.lab.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.emp.entity.CmsArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CmsArticleMapper extends BaseMapper<CmsArticle> {

    /** 控制台：按状态统计文章数量 */
    @Select("SELECT status, COUNT(*) AS cnt, COALESCE(SUM(view_count), 0) AS views "
            + "FROM cms_article WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> countGroupByStatus();

    /** 控制台：各分类文章数（含空分类） */
    @Select("SELECT c.category_id AS categoryId, c.category_name AS categoryName, COUNT(a.article_id) AS cnt "
            + "FROM cms_category c LEFT JOIN cms_article a "
            + "  ON a.category_id = c.category_id AND a.deleted = 0 AND a.status = 'PUBLISHED' "
            + "WHERE c.deleted = 0 GROUP BY c.category_id, c.category_name ORDER BY c.sort")
    List<Map<String, Object>> countByCategory();

    /** 分类下文章数，用于删除分类前的占用校验 */
    @Select("SELECT COUNT(*) FROM cms_article WHERE category_id = #{categoryId} AND deleted = 0")
    int countArticleByCategory(Integer categoryId);
}