package com.lab.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.emp.dto.DeptStatDTO;
import com.lab.emp.entity.Emp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工持久层。
 * <p>继承 BaseMapper 获得单表 CRUD 与条件构造器能力；联表/聚合查询写在 mapper/EmpMapper.xml 中。</p>
 */
public interface EmpMapper extends BaseMapper<Emp> {

    /**
     * 按主键查询并携带部门名称。
     */
    Emp selectByIdWithDept(@Param("empId") Integer empId);

    /**
     * 部门维度统计：人数、平均薪资、最高薪资。
     */
    List<DeptStatDTO> statByDept();
}