package com.lab.emp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.emp.common.BusinessException;
import com.lab.emp.common.PageResult;
import com.lab.emp.common.ResultCode;
import com.lab.emp.dto.UserSaveDTO;
import com.lab.emp.dto.UserVO;
import com.lab.emp.entity.SysPermission;
import com.lab.emp.entity.SysRole;
import com.lab.emp.entity.SysUser;
import com.lab.emp.mapper.RolePermissionMapper;
import com.lab.emp.mapper.SysPermissionMapper;
import com.lab.emp.mapper.SysRoleMapper;
import com.lab.emp.mapper.SysUserMapper;
import com.lab.emp.mapper.UserRoleMapper;
import com.lab.emp.service.RbacService;
import com.lab.emp.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RBAC 管理实现：用户维护、角色分配、角色权限重设。
 * <p>用户列表的角色名用一次批量查询回填，避免每行再查一次角色。</p>
 */
@Service
public class RbacServiceImpl implements RbacService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    public RbacServiceImpl(SysUserMapper userMapper,
                           SysRoleMapper roleMapper,
                           SysPermissionMapper permissionMapper,
                           UserRoleMapper userRoleMapper,
                           RolePermissionMapper rolePermissionMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    @Override
    public PageResult<UserVO> pageUsers(String keyword, int page, int size) {
        int current = Math.max(page, 1);
        int pageSize = size < 1 ? 10 : Math.min(size, 100);
        String kw = keyword == null ? null : keyword.trim();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (kw != null && !kw.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, kw).or().like(SysUser::getRealName, kw));
        }
        wrapper.orderByDesc(SysUser::getUserId);

        Page<SysUser> userPage = userMapper.selectPage(new Page<>(current, pageSize), wrapper);
        Page<UserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(toVoList(userPage.getRecords()));
        return PageResult.of(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO saveUser(UserSaveDTO dto) {
        checkUsernameUnique(dto.getUsername(), null);
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "新增用户时必须设置初始口令");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(PasswordUtil.sha256Hex(dto.getPassword()));
        user.setRealName(dto.getRealName().trim());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        userMapper.insert(user);

        replaceRoles(user.getUserId(), dto.getRoleIds());
        return toVo(user, dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(UserSaveDTO dto) {
        SysUser exists = requireUser(dto.getUserId());
        checkUsernameUnique(dto.getUsername(), exists.getUserId());

        SysUser user = new SysUser();
        user.setUserId(exists.getUserId());
        user.setUsername(dto.getUsername().trim());
        user.setRealName(dto.getRealName().trim());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(PasswordUtil.sha256Hex(dto.getPassword()));
        }
        userMapper.updateById(user);

        if (dto.getRoleIds() != null) {
            replaceRoles(exists.getUserId(), dto.getRoleIds());
        }
        List<Integer> roleIds = dto.getRoleIds() != null
                ? dto.getRoleIds() : userRoleMapper.selectRoleIdsByUserId(exists.getUserId());
        return toVo(user, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Integer userId, List<Integer> roleIds) {
        requireUser(userId);
        replaceRoles(userId, roleIds);
    }

    @Override
    public List<SysRole> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getRoleId));
    }

    @Override
    public List<SysPermission> permissionTree() {
        List<SysPermission> all = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .orderByAsc(SysPermission::getSort));
        Map<Integer, SysPermission> index = new HashMap<>();
        for (SysPermission p : all) {
            p.setChildren(new ArrayList<>());
            index.put(p.getPermId(), p);
        }
        List<SysPermission> roots = new ArrayList<>();
        for (SysPermission p : all) {
            SysPermission parent = p.getParentId() == null ? null : index.get(p.getParentId());
            if (parent == null || p.getParentId() == 0) {
                roots.add(p);
            } else {
                parent.getChildren().add(p);
            }
        }
        return roots;
    }

    @Override
    public List<Integer> permissionIdsOfRole(Integer roleId) {
        requireRole(roleId);
        return permissionMapper.selectPermIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRolePermissions(Integer roleId, List<Integer> permIds) {
        requireRole(roleId);
        rolePermissionMapper.deleteByRoleId(roleId);
        Set<Integer> unique = permIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(permIds);
        if (!unique.isEmpty()) {
            rolePermissionMapper.insertBatch(roleId, new ArrayList<>(unique));
        }
        return unique.size();
    }

    // ------------------------------------------------------------------ 私有方法

    private void replaceRoles(Integer userId, List<Integer> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Set<Integer> unique = new LinkedHashSet<>(roleIds);
        for (Integer roleId : unique) {
            requireRole(roleId);
        }
        userRoleMapper.insertBatch(userId, new ArrayList<>(unique));
    }

    private SysUser requireUser(Integer userId) {
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在，编号=" + userId);
        }
        return user;
    }

    private SysRole requireRole(Integer roleId) {
        SysRole role = roleId == null ? null : roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "角色不存在，编号=" + roleId);
        }
        return role;
    }

    private void checkUsernameUnique(String username, Integer excludeId) {
        SysUser other = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim())
                .last("LIMIT 1"));
        if (other != null && !other.getUserId().equals(excludeId)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "登录名已存在：" + username);
        }
    }

    private List<UserVO> toVoList(List<SysUser> users) {
        List<UserVO> list = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return list;
        }
        List<Integer> ids = new ArrayList<>();
        for (SysUser u : users) {
            ids.add(u.getUserId());
        }
        Map<Integer, List<Integer>> roleIds = new HashMap<>();
        Map<Integer, List<String>> roleNames = new HashMap<>();
        for (Map<String, Object> row : userRoleMapper.selectRolesByUserIds(ids)) {
            Integer userId = ((Number) row.get("userId")).intValue();
            Integer roleId = ((Number) row.get("roleId")).intValue();
            roleIds.computeIfAbsent(userId, k -> new ArrayList<>()).add(roleId);
            roleNames.computeIfAbsent(userId, k -> new ArrayList<>()).add(String.valueOf(row.get("roleName")));
        }
        for (SysUser u : users) {
            UserVO vo = baseVo(u);
            vo.setRoleIds(roleIds.getOrDefault(u.getUserId(), new ArrayList<>()));
            vo.setRoleNames(roleNames.getOrDefault(u.getUserId(), new ArrayList<>()));
            list.add(vo);
        }
        return list;
    }

    private UserVO toVo(SysUser user, List<Integer> roleIds) {
        UserVO vo = baseVo(user);
        List<Integer> ids = roleIds == null ? new ArrayList<>() : roleIds;
        vo.setRoleIds(ids);
        List<String> names = new ArrayList<>();
        for (Integer roleId : ids) {
            SysRole role = roleMapper.selectById(roleId);
            names.add(role == null ? String.valueOf(roleId) : role.getRoleName());
        }
        vo.setRoleNames(names);
        return vo;
    }

    private UserVO baseVo(SysUser u) {
        UserVO vo = new UserVO();
        vo.setUserId(u.getUserId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setEmail(u.getEmail());
        vo.setPhone(u.getPhone());
        vo.setStatus(u.getStatus());
        vo.setCreateTime(u.getCreateTime() == null ? null : u.getCreateTime().toString().replace('T', ' '));
        return vo;
    }
}