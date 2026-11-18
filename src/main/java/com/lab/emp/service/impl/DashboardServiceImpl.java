package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.emp.dto.DashboardVO;
import com.lab.emp.entity.CmsArticle;
import com.lab.emp.entity.CmsCategory;
import com.lab.emp.entity.SysUser;
import com.lab.emp.mapper.CmsArticleMapper;
import com.lab.emp.mapper.CmsCategoryMapper;
import com.lab.emp.mapper.SysUserMapper;
import com.lab.emp.service.DashboardService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 控制台统计：两次分组查询 + 三次 count，全部走索引列，避免全表扫描。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final CmsArticleMapper articleMapper;
    private final CmsCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;

    public DashboardServiceImpl(CmsArticleMapper articleMapper,
                                CmsCategoryMapper categoryMapper,
                                SysUserMapper userMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    @Override
    public DashboardVO stats() {
        DashboardVO vo = new DashboardVO();
        long totalViews = 0;
        long total = 0;
        List<Map<String, Object>> statusRows = articleMapper.countGroupByStatus();
        for (Map<String, Object> row : statusRows) {
            String status = String.valueOf(row.get("status"));
            long cnt = ((Number) row.get("cnt")).longValue();
            total += cnt;
            totalViews += ((Number) row.get("views")).longValue();
            switch (status) {
                case "PUBLISHED": vo.setPublished(cnt); break;
                case "PENDING": vo.setPending(cnt); break;
                case "DRAFT": vo.setDraft(cnt); break;
                case "REJECTED": vo.setRejected(cnt); break;
                case "OFFLINE": vo.setOffline(cnt); break;
                default: break;
            }
        }
        vo.setTotalArticles(total);
        vo.setTotalViews(totalViews);
        vo.setTotalCategories(categoryMapper.selectCount(new LambdaQueryWrapper<CmsCategory>()));
        vo.setTotalUsers(userMapper.selectCount(new LambdaQueryWrapper<SysUser>()));
        vo.setByCategory(articleMapper.countByCategory());
        return vo;
    }
}