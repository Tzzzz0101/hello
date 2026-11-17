package com.lab.emp;

import com.lab.emp.config.SpringConfig;
import com.lab.emp.entity.Emp;
import com.lab.emp.service.EmpService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 事务演示测试（不加 @Transactional，让被测方法自己管理事务，才能观察到真实提交/回滚结果）。
 * <p>验证结论：rollback 场景数据不落库；swallow（异常被吞）、self（this 自调用）、
 * private（私有方法注解）三个场景数据都会落库，即事务失效。</p>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
public class TransactionDemoTest {

    private static final String PREFIX = "UT-TX-";

    @Autowired
    private EmpService empService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    private JdbcTemplate jdbc() {
        if (jdbcTemplate == null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
        return jdbcTemplate;
    }

    @After
    public void cleanUp() {
        jdbc().update("DELETE FROM emp WHERE emp_name LIKE ?", PREFIX + "%");
    }

    @Test
    public void saveWithTxShouldRollback() {
        String name = PREFIX + "rollback";
        try {
            empService.saveWithTx(buildEmp(name));
            fail("应当抛出算术异常");
        } catch (ArithmeticException expected) {
            assertTrue("异常信息应为 / by zero", expected.getMessage().contains("by zero"));
        }
        assertEquals("事务应回滚，库中不应有该记录", 0L, empService.countByName(name));
    }

    @Test
    public void swallowedExceptionShouldCommit() {
        String name = PREFIX + "swallow";
        empService.txSwallowException(buildEmp(name));
        assertEquals("异常被吞掉，记录已提交", 1L, empService.countByName(name));
    }

    @Test
    public void selfInvocationShouldNotRollback() {
        String name = PREFIX + "self";
        try {
            empService.txSelfInvocation(buildEmp(name));
            fail("应当抛出算术异常");
        } catch (ArithmeticException expected) {
            assertFalse(expected.getMessage().isEmpty());
        }
        assertEquals("this 自调用使事务失效，记录已提交", 1L, empService.countByName(name));
    }

    @Test
    public void privateMethodAnnotationShouldNotRollback() {
        String name = PREFIX + "private";
        try {
            empService.txPrivateMethod(buildEmp(name));
            fail("应当抛出算术异常");
        } catch (ArithmeticException expected) {
            assertFalse(expected.getMessage().isEmpty());
        }
        assertEquals("私有方法上的 @Transactional 不生效，记录已提交", 1L, empService.countByName(name));
    }

    private Emp buildEmp(String name) {
        Emp emp = new Emp();
        emp.setEmpName(name);
        emp.setGender("男");
        emp.setDeptId(1);
        emp.setPost("事务测试");
        emp.setSalary(new BigDecimal("6000.00"));
        emp.setHireDate(LocalDate.now());
        emp.setStatus(1);
        return emp;
    }
}