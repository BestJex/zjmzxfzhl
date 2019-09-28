package com.zjmzxfzhl.modules.sys.service;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zjmzxfzhl.common.Constants;
import com.zjmzxfzhl.common.base.BaseService;
import com.zjmzxfzhl.common.exception.SysException;
import com.zjmzxfzhl.common.util.RedisUtil;
import com.zjmzxfzhl.modules.sys.entity.SysCodeInfo;
import com.zjmzxfzhl.modules.sys.entity.SysCodeType;
import com.zjmzxfzhl.modules.sys.mapper.SysCodeTypeMapper;

/**
 * 代码类别Service
 * 
 * @author 庄金明
 */
@Service
public class SysCodeTypeService extends BaseService<SysCodeTypeMapper, SysCodeType> {

	@Autowired
	private SysCodeInfoService sysCodeInfoService;

	@Autowired
	private RedisUtil redisUtil;

	public IPage<SysCodeType> list(IPage<SysCodeType> page, SysCodeType sysCodeType) {
		return page.setRecords(baseMapper.list(page, sysCodeType));
	}

	/**
	 * 删除数据字典信息
	 * 
	 * @param ids
	 */
	public void deleteSysCodeType(String ids) {
		if (ids == null || ids.trim().length() == 0) {
			throw new SysException("ids can't be empty");
		}
		String[] idsArr = ids.split(",");
		if (idsArr.length > 1) {
			removeByIds(Arrays.asList(idsArr));
		} else {
			removeById(idsArr[0]);
		}
		sysCodeInfoService.remove(new QueryWrapper<SysCodeInfo>().in("code_type_id", (Object[]) idsArr));

		for (String codeTypeId : idsArr) {
			redisUtil.del(Constants.PREFIX_SYS_CODE_TYPE + codeTypeId);
		}
	}
}
