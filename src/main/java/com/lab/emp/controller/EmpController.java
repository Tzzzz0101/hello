package com.lab.emp.controller;

import com.lab.emp.common.BusinessException;
import com.lab.emp.common.PageResult;
import com.lab.emp.common.Result;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.DeptStatDTO;
import com.lab.emp.dto.EmpQueryDTO;
import com.lab.emp.dto.EmpSaveDTO;
import com.lab.emp.dto.EmpUpdateDTO;
import com.lab.emp.dto.EmpVO;
import com.lab.emp.entity.Emp;
import com.lab.emp.service.EmpService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 员工管理 REST 接口。
 * <p>HTTP 语义：GET 查询、POST 新增、PUT 全量修改、DELETE 删除；
 * 所有响应统一为 Result&lt;T&gt;。</p>
 */
@RestController
@RequestMapping("/emps")
public class EmpController {

    private final EmpService empService;

    public EmpController(EmpService empService) {
        this.empService = empService;
    }

    /** 分页 + 多条件查询：GET /emps?name=&gender=&deptId=&status=&page=1&size=5 */
    @GetMapping
    public Result<PageResult<EmpVO>> list(EmpQueryDTO query) {
        return Result.success(empService.pageQuery(query));
    }

    /** 查询详情：GET /emps/{id} */
    @GetMapping("/{id}")
    public Result<EmpVO> detail(@PathVariable("id") Integer id) {
        return Result.success(empService.getById(id));
    }

    /** 新增：POST /emps */
    @PostMapping
    public Result<EmpVO> create(@Valid @RequestBody EmpSaveDTO dto, BindingResult bindingResult) {
        checkBinding(bindingResult);
        return Result.success("新增成功", empService.save(dto));
    }

    /** 修改：PUT /emps */
    @PutMapping
    public Result<EmpVO> update(@Valid @RequestBody EmpUpdateDTO dto, BindingResult bindingResult) {
        checkBinding(bindingResult);
        return Result.success("修改成功", empService.update(dto));
    }

    /** 删除单条：DELETE /emps/{id} */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Integer id) {
        empService.deleteById(id);
        return Result.success("删除成功", null);
    }

    /** 批量删除：DELETE /emps，请求体为编号数组 [1,2,3] */
    @DeleteMapping
    public Result<Integer> deleteBatch(@RequestBody(required = false) List<Integer> ids) {
        int rows = empService.deleteBatch(ids);
        return Result.success("已删除 " + rows + " 条记录", rows);
    }

    /** 部门统计：GET /emps/stat/dept */
    @GetMapping("/stat/dept")
    public Result<List<DeptStatDTO>> statByDept() {
        return Result.success(empService.statByDept());
    }

    /**
     * 事务演示接口：POST /emps/tx-demo?scenario=rollback|swallow|self|private
     * <p>返回 persisted / rolledBack 两个布尔值，用于观察四个场景下数据是否落库。</p>
     */
    @PostMapping("/tx-demo")
    public Result<Map<String, Object>> txDemo(@RequestParam(value = "scenario", defaultValue = "rollback") String scenario,
                                              @RequestParam(value = "empName", required = false) String empName) {
        String name = (empName == null || empName.trim().isEmpty())
                ? "事务演示员工-" + scenario : empName.trim();

        Emp emp = new Emp();
        emp.setEmpName(name);
        emp.setGender("男");
        emp.setDeptId(1);
        emp.setPost("事务演示");
        emp.setSalary(new BigDecimal("6000.00"));
        emp.setHireDate(LocalDate.now());
        emp.setStatus(1);

        String exception = null;
        try {
            if ("rollback".equals(scenario)) {
                empService.saveWithTx(emp);
            } else if ("swallow".equals(scenario)) {
                empService.txSwallowException(emp);
            } else if ("self".equals(scenario)) {
                empService.txSelfInvocation(emp);
            } else if ("private".equals(scenario)) {
                empService.txPrivateMethod(emp);
            } else {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(),
                        "未知场景：" + scenario + "，可选 rollback / swallow / self / private");
            }
        } catch (Exception e) {
            exception = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        long persisted = empService.countByName(name);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scenario", scenario);
        data.put("empName", name);
        data.put("exception", exception);
        data.put("persistedCount", persisted);
        data.put("persisted", persisted > 0);
        data.put("rolledBack", persisted == 0);
        return Result.success(data);
    }

    /** 把 BindingResult 中的校验消息转成业务异常，交由全局异常处理器统一输出 */
    private void checkBinding(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("；"));
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
        }
    }
}