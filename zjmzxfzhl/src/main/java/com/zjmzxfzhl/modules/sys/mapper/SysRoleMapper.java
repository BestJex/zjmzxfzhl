package com.zjmzxfzhl.modules.sys.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zjmzxfzhl.modules.sys.entity.SysRole;
import com.zjmzxfzhl.modules.sys.entity.SysRoleUser;
import com.zjmzxfzhl.modules.sys.entity.SysUser;

/**
 * 角色Mapper
 * 
 * @author 庄金明
 */
public interface SysRoleMapper extends BaseMapper<SysRole> {
	public List<SysRole> list(IPage<SysRole> page, @Param("entity") SysRole entity);

	public List<String> listMenuOrFuncIdsByRoleId(String roleId);

	public List<SysUser> getRoleUser(IPage<SysUser> page, @Param("entity") SysRoleUser entity);
}
