package com.lab.emp.service;

import com.lab.emp.common.PageResult;
import com.lab.emp.dto.DeptStatDTO;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.dto.EmpSaveDTO;
import com.lab.emp.dto.EmpUpdateDTO;
import com.lab.emp.dto.EmpVO;
import com.lab.emp.entity.Emp;

import java.util.List;

/**
 * 员工业务接口。
 * <p>Controller 只依赖接口，便于替换实现与编写单元测试。</p>
 */
public interface EmpService {

    /** 多条件分页查询 */
    PageResult<EmpVO> pageQuery(EmpQueryDTO query);

    /** 按主键查询详情（含部门名称） */
    EmpVO getById(Integer empId);

    /** 新增员工 */
    EmpVO save(EmpSaveDTO dto);

    /** 修改员工 */
    EmpVO update(EmpUpdateDTO dto);

    /** 按主键删除（逻辑删除） */
    void deleteById(Integer empId);

    /** 批量删除，返回实际影响行数 */
    int deleteBatch(List<Integer> ids);

    /** 部门维度统计 */
    List<DeptStatDTO> statByDept();

    /** 统计指定姓名的记录数，用于事务演示的效果验证 */
    long countByName(String empName);

    // ------------------------------------------------------------------
    // 任务 6.5：声明式事务验证（一个正确用法 + 三个失效场景）
    // ------------------------------------------------------------------

    /** 正确用法：插入后抛异常，整体回滚 */
    void saveWithTx(Emp emp);

    /** 失效场景一：异常被 try/catch 吞掉，事务提交 */
    void txSwallowException(Emp emp);

    /** 失效场景二：this 自调用，事务注解不生效（未经过代理） */
    void txSelfInvocation(Emp emp);

    /** 失效场景三：@Transactional 标注在 private 方法上，代理无法拦截 */
    void txPrivateMethod(Emp emp);
}