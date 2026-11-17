package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.BusinessException;
import com.lab.emp.common.PageResult;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.DeptStatDTO;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.dto.EmpSaveDTO;
import com.lab.emp.dto.EmpUpdateDTO;
import com.lab.emp.dto.EmpVO;
import com.lab.emp.entity.Dept;
import com.lab.emp.entity.Emp;
import com.lab.emp.mapper.DeptMapper;
import com.lab.emp.mapper.EmpMapper;
import com.lab.emp.service.EmpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 员工业务实现。
 * <p>查询统一走 MyBatis-Plus 条件构造器（Lambda 形式，字段名由方法引用推导，编译期即可发现改名错误）；
 * 联表与聚合查询走 XML 映射。</p>
 */
@Service
public class EmpServiceImpl implements EmpService {

    private static final Logger log = LoggerFactory.getLogger(EmpServiceImpl.class);

    /** 默认每页条数 */
    private static final int DEFAULT_PAGE_SIZE = 5;
    /** 单页上限，防止恶意放大 size 造成全表扫描 */
    private static final int MAX_PAGE_SIZE = 100;

    private final EmpMapper empMapper;
    private final DeptMapper deptMapper;

    public EmpServiceImpl(EmpMapper empMapper, DeptMapper deptMapper) {
        this.empMapper = empMapper;
        this.deptMapper = deptMapper;
    }

    @Override
    public PageResult<EmpVO> pageQuery(EmpQueryDTO query) {
        EmpQueryDTO condition = query == null ? new EmpQueryDTO() : query;

        // 防御性归一化：非法页码/条数不报错，而是纠正为可用值
        long page = (condition.getPage() == null || condition.getPage() < 1) ? 1 : condition.getPage();
        long size = (condition.getSize() == null || condition.getSize() < 1)
                ? DEFAULT_PAGE_SIZE : Math.min(condition.getSize(), MAX_PAGE_SIZE);

        LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(condition.getName()), Emp::getEmpName, condition.getName())
                .eq(StringUtils.hasText(condition.getGender()), Emp::getGender, condition.getGender())
                .eq(condition.getDeptId() != null, Emp::getDeptId, condition.getDeptId())
                .eq(condition.getStatus() != null, Emp::getStatus, condition.getStatus())
                .orderByDesc(Emp::getEmpId);

        IPage<Emp> result = empMapper.selectPage(new Page<>(page, size), wrapper);
        List<EmpVO> records = toVoList(result.getRecords());
        log.debug("分页查询条件={} 命中 {} 条", condition, result.getTotal());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(),
                result.getPages(), records);
    }

    @Override
    public EmpVO getById(Integer empId) {
        if (empId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "员工编号不能为空");
        }
        Emp emp = empMapper.selectByIdWithDept(empId);
        if (emp == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在，编号=" + empId);
        }
        return EmpVO.from(emp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmpVO save(EmpSaveDTO dto) {
        checkDeptExists(dto.getDeptId());
        checkNameUnique(dto.getEmpName(), null);

        Emp emp = new Emp();
        emp.setEmpName(dto.getEmpName().trim());
        emp.setGender(dto.getGender());
        emp.setDeptId(dto.getDeptId());
        emp.setPost(dto.getPost());
        emp.setSalary(dto.getSalary());
        emp.setHireDate(dto.getHireDate());
        emp.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        empMapper.insert(emp);
        log.info("新增员工成功: id={}, name={}", emp.getEmpId(), emp.getEmpName());
        return getById(emp.getEmpId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmpVO update(EmpUpdateDTO dto) {
        Emp existing = empMapper.selectById(dto.getEmpId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在，编号=" + dto.getEmpId());
        }
        checkDeptExists(dto.getDeptId());
        checkNameUnique(dto.getEmpName(), dto.getEmpId());

        Emp emp = new Emp();
        emp.setEmpId(dto.getEmpId());
        emp.setEmpName(dto.getEmpName().trim());
        emp.setGender(dto.getGender());
        emp.setDeptId(dto.getDeptId());
        emp.setPost(dto.getPost());
        emp.setSalary(dto.getSalary());
        emp.setHireDate(dto.getHireDate());
        emp.setStatus(dto.getStatus());
        empMapper.updateById(emp);
        log.info("修改员工成功: id={}, name={}", dto.getEmpId(), dto.getEmpName());
        return getById(dto.getEmpId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer empId) {
        if (empId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "员工编号不能为空");
        }
        Emp existing = empMapper.selectById(empId);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "员工不存在，编号=" + empId);
        }
        int rows = empMapper.deleteById(empId);
        log.info("删除员工: id={}, 影响行数={}", empId, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择要删除的员工");
        }
        List<Integer> distinctIds = ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择要删除的员工");
        }
        int rows = empMapper.deleteBatchIds(distinctIds);
        log.info("批量删除员工: ids={}, 影响行数={}", distinctIds, rows);
        return rows;
    }

    @Override
    public List<DeptStatDTO> statByDept() {
        return empMapper.statByDept();
    }

    @Override
    public long countByName(String empName) {
        if (!StringUtils.hasText(empName)) {
            return 0L;
        }
        return empMapper.selectCount(new LambdaQueryWrapper<Emp>().eq(Emp::getEmpName, empName));
    }

    // ==================================================================
    // 任务 6.5 事务演示
    // ==================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithTx(Emp emp) {
        empMapper.insert(emp);
        log.info("saveWithTx 已插入 id={}，随即抛出算术异常，期望回滚", emp.getEmpId());
        // 故意制造运行时异常，验证 rollbackFor = Exception.class 生效
        int divideByZero = 1 / 0;
        log.info("不会执行到这里: {}", divideByZero);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void txSwallowException(Emp emp) {
        empMapper.insert(emp);
        try {
            int divideByZero = 1 / 0;
            log.info("不会执行到这里: {}", divideByZero);
        } catch (ArithmeticException e) {
            // 异常被吞掉：Spring 感知不到失败，事务正常提交 —— 这是最常见的失效场景
            log.warn("事务演示：异常被 catch 吞掉，事务将提交，异常信息={}", e.getMessage());
        }
    }

    @Override
    public void txSelfInvocation(Emp emp) {
        // this 调用不经过代理对象，insertAndFail 上的 @Transactional 完全失效
        this.insertAndFail(emp);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void insertAndFail(Emp emp) {
        empMapper.insert(emp);
        log.info("insertAndFail 已插入 id={}（未被事务管理）", emp.getEmpId());
        throw new ArithmeticException("/ by zero");
    }

    @Override
    public void txPrivateMethod(Emp emp) {
        insertInPrivateTransaction(emp);
    }

    /**
     * 私有方法上的 @Transactional 不会生效：JDK/CGLIB 代理只能拦截可覆写的 public 方法，
     * 且 Spring 在解析注解时会忽略非 public 方法。
     */
    @Transactional(rollbackFor = Exception.class)
    private void insertInPrivateTransaction(Emp emp) {
        empMapper.insert(emp);
        log.info("私有方法已插入 id={}（未被事务管理）", emp.getEmpId());
        throw new ArithmeticException("/ by zero");
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    /** 批量补齐部门名称，避免 N+1 查询 */
    private List<EmpVO> toVoList(List<Emp> emps) {
        if (emps == null || emps.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> deptIds = emps.stream()
                .map(Emp::getDeptId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<Dept> depts = deptMapper.selectBatchIds(deptIds);
            for (Dept dept : depts) {
                deptNameMap.put(dept.getDeptId(), dept.getDeptName());
            }
        }
        List<EmpVO> records = new ArrayList<>(emps.size());
        for (Emp emp : emps) {
            EmpVO vo = EmpVO.from(emp);
            if (vo != null) {
                vo.setDeptName(deptNameMap.get(emp.getDeptId()));
                records.add(vo);
            }
        }
        return records;
    }

    private void checkDeptExists(Integer deptId) {
        if (deptId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "所属部门不能为空");
        }
        if (deptMapper.selectById(deptId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "所选部门不存在，编号=" + deptId);
        }
    }

    private void checkNameUnique(String empName, Integer excludeEmpId) {
        LambdaQueryWrapper<Emp> wrapper = new LambdaQueryWrapper<Emp>()
                .eq(Emp::getEmpName, empName == null ? null : empName.trim());
        if (excludeEmpId != null) {
            wrapper.ne(Emp::getEmpId, excludeEmpId);
        }
        if (empMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "员工姓名已存在：" + empName);
        }
    }
}