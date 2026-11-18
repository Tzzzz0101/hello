package com.lab.emp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限：MENU 用于导航渲染，API 用于后端接口鉴权；parentId 组织成树。
 */
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Integer permId;
    private String permCode;
    private String permName;
    private String permType;
    private Integer parentId;
    private String path;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;

    /** 非数据库字段：组装树时使用 */
    @TableField(exist = false)
    private List<SysPermission> children = new ArrayList<>();

    public Integer getPermId() { return permId; }
    public void setPermId(Integer permId) { this.permId = permId; }

    public String getPermCode() { return permCode; }
    public void setPermCode(String permCode) { this.permCode = permCode; }

    public String getPermName() { return permName; }
    public void setPermName(String permName) { this.permName = permName; }

    public String getPermType() { return permType; }
    public void setPermType(String permType) { this.permType = permType; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public List<SysPermission> getChildren() { return children; }
    public void setChildren(List<SysPermission> children) { this.children = children; }
}