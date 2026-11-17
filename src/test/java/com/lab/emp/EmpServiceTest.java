package com.lab.emp;

import com.lab.emp.common.PageResult;
import com.lab.emp.config.SpringConfig;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.dto.EmpSaveDTO;
import com.lab.emp.dto.EmpUpdateDTO;
import com.lab.emp.dto.EmpVO;
import com.lab.emp.common.BusinessException;
import com.lab.emp.service.EmpService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 业务层集成测试：启动真实的 Spring 容器与 Druid 数据源，验证分页查询、新增、修改、
 * 参数校验与批量删除。测试方法标注 @Transactional，结束后自动回滚，不污染实验数据。
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@Transactional
public class EmpServiceTest {

    @Autowired
    private EmpService empService;

    @Test
    public void pageQueryShouldReturnDeptNameAndPagedRecords() {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setName("张");
        query.setPage(1);
        query.setSize(5);

        PageResult<EmpVO> result = empService.pageQuery(query);

        assertNotNull(result.getRecords());
        assertTrue("总记录数应大于 0", result.getTotal() > 0);
        assertTrue("每页条数应为 5 以内", result.getRecords().size() <= 5);
        assertNotNull("部门名称应由关联查询补齐", result.getRecords().get(0).getDeptName());
    }

    @Test
    public void pageQueryShouldNormalizeIllegalPageParams() {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setPage(0);
        query.setSize(-3);

        PageResult<EmpVO> result = empService.pageQuery(query);

        assertEquals("page<1 应被纠正为第 1 页", 1L, result.getPage());
        assertEquals("size<1 应回落到默认条数", 5L, result.getSize());
    }

    @Test
    public void saveAndUpdateShouldRoundTrip() {
        EmpSaveDTO saveDTO = new EmpSaveDTO();
        saveDTO.setEmpName("单元测试员工");
        saveDTO.setGender("女");
        saveDTO.setDeptId(1);
        saveDTO.setPost("测试岗");
        saveDTO.setSalary(new BigDecimal("8888.88"));
        saveDTO.setHireDate(LocalDate.of(2025, 1, 1));
        saveDTO.setStatus(1);

        EmpVO created = empService.save(saveDTO);
        assertNotNull("新增后应返回主键", created.getEmpId());
        assertEquals("单元测试员工", created.getEmpName());
        assertEquals("研发部", created.getDeptName());

        EmpUpdateDTO updateDTO = new EmpUpdateDTO();
        updateDTO.setEmpId(created.getEmpId());
        updateDTO.setEmpName("单元测试员工-改");
        updateDTO.setGender("女");
        updateDTO.setDeptId(2);
        updateDTO.setPost("测试岗");
        updateDTO.setSalary(new BigDecimal("9999.00"));
        updateDTO.setHireDate(LocalDate.of(2025, 1, 1));
        updateDTO.setStatus(1);

        EmpVO updated = empService.update(updateDTO);
        assertEquals("单元测试员工-改", updated.getEmpName());
        assertEquals("市场部", updated.getDeptName());
        assertEquals(0, new BigDecimal("9999.00").compareTo(updated.getSalary()));
    }

    @Test(expected = BusinessException.class)
    public void deleteBatchWithEmptyIdsShouldFail() {
        empService.deleteBatch(Collections.emptyList());
    }

    @Test(expected = BusinessException.class)
    public void getByMissingIdShouldFail() {
        empService.getById(999999);
    }
}