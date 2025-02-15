package cn.com.mfish.oauth.controller;

import cn.com.mfish.common.core.enums.OperateType;
import cn.com.mfish.common.core.exception.MyRuntimeException;
import cn.com.mfish.common.core.utils.AuthInfoUtils;
import cn.com.mfish.common.core.utils.StringUtils;
import cn.com.mfish.common.core.utils.TreeUtils;
import cn.com.mfish.common.core.utils.excel.ExcelUtils;
import cn.com.mfish.common.core.web.PageResult;
import cn.com.mfish.common.core.web.ReqPage;
import cn.com.mfish.common.core.web.Result;
import cn.com.mfish.common.log.annotation.Log;
import cn.com.mfish.common.oauth.annotation.RequiresPermissions;
import cn.com.mfish.common.oauth.api.entity.SsoOrg;
import cn.com.mfish.common.oauth.api.entity.SsoTenant;
import cn.com.mfish.common.oauth.api.entity.UserInfo;
import cn.com.mfish.common.oauth.api.vo.TenantVo;
import cn.com.mfish.common.oauth.common.OauthUtils;
import cn.com.mfish.common.oauth.entity.RedisAccessToken;
import cn.com.mfish.common.oauth.entity.WeChatToken;
import cn.com.mfish.oauth.cache.common.ClearCache;
import cn.com.mfish.oauth.entity.SsoMenu;
import cn.com.mfish.oauth.entity.SsoRole;
import cn.com.mfish.oauth.entity.UserOrg;
import cn.com.mfish.oauth.req.*;
import cn.com.mfish.oauth.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description: 租户信息表
 * @author: mfish
 * @date: 2023-05-31
 * @version: V1.0.0
 */
@Slf4j
@Api(tags = "租户信息表")
@RestController
@RequestMapping("/ssoTenant")
public class SsoTenantController {
    @Resource
    SsoTenantService ssoTenantService;
    @Resource
    SsoOrgService ssoOrgService;
    @Resource
    SsoRoleService ssoRoleService;
    @Resource
    SsoMenuService ssoMenuService;
    @Resource
    SsoUserService ssoUserService;
    @Resource
    ClearCache clearCache;

    /**
     * 分页列表查询
     *
     * @param reqSsoTenant 租户信息表请求参数
     * @param reqPage      分页参数
     * @return 返回租户信息表-分页列表
     */
    @ApiOperation(value = "租户信息表-分页列表查询", notes = "租户信息表-分页列表查询")
    @GetMapping
    @RequiresPermissions("sys:ssoTenant:query")
    public Result<PageResult<TenantVo>> queryPageList(ReqSsoTenant reqSsoTenant, ReqPage reqPage) {
        return Result.ok(new PageResult<>(queryList(reqSsoTenant, reqPage)), "租户信息表-查询成功!");
    }

    /**
     * 获取列表
     *
     * @param reqSsoTenant 租户信息表请求参数
     * @param reqPage      分页参数
     * @return 返回租户信息表-分页列表
     */
    private List<TenantVo> queryList(ReqSsoTenant reqSsoTenant, ReqPage reqPage) {
        return ssoTenantService.queryList(reqSsoTenant, reqPage);
    }

    /**
     * 获取当前租户信息
     *
     * @return 租户信息
     */
    @ApiOperation(value = "获取当前租户信息", notes = "获取当前租户信息")
    @GetMapping("/info")
    public Result<TenantVo> queryTenantInfo() {
        return Result.ok(ssoTenantService.queryInfo(AuthInfoUtils.getCurrentTenantId()), "租户信息-查询成功!");
    }

    /**
     * 添加
     *
     * @param ssoTenant 租户信息表对象
     * @return 返回租户信息表-添加结果
     */
    @Log(title = "租户信息表-添加", operateType = OperateType.INSERT)
    @ApiOperation("租户信息表-添加")
    @PostMapping
    @RequiresPermissions("sys:ssoTenant:insert")
    public Result<SsoTenant> add(@RequestBody SsoTenant ssoTenant) {
        return ssoTenantService.insertTenant(ssoTenant);
    }

    /**
     * 编辑
     *
     * @param ssoTenant 租户信息表对象
     * @return 返回租户信息表-编辑结果
     */
    @Log(title = "租户信息表-编辑", operateType = OperateType.UPDATE)
    @ApiOperation("租户信息表-编辑")
    @PutMapping
    @RequiresPermissions("sys:ssoTenant:update")
    public Result<SsoTenant> edit(@RequestBody SsoTenant ssoTenant) {
        return ssoTenantService.updateTenant(ssoTenant);
    }

    @Log(title = "管理员编辑自己租户信息", operateType = OperateType.UPDATE)
    @ApiOperation("管理员编辑自己租户信息")
    @PutMapping("/me")
    public Result<SsoTenant> editMe(@RequestBody SsoTenant ssoTenant) {
        if (ssoTenantService.isTenantMaster(ssoTenant.getUserId(), ssoTenant.getId())) {
            return ssoTenantService.updateTenant(ssoTenant);
        }
        return Result.fail(ssoTenant, "错误:只允许管理员修改");
    }

    /**
     * 通过id删除
     *
     * @param id 唯一ID
     * @return 返回租户信息表-删除结果
     */
    @Log(title = "租户信息表-通过id删除", operateType = OperateType.DELETE)
    @ApiOperation("租户信息表-通过id删除")
    @DeleteMapping("/{id}")
    @RequiresPermissions("sys:ssoTenant:delete")
    public Result<Boolean> delete(@ApiParam(name = "id", value = "唯一性ID") @PathVariable String id) {
        return ssoTenantService.deleteTenant(id);
    }

    /**
     * 通过id查询
     *
     * @param id 唯一ID
     * @return 返回租户信息表对象
     */
    @ApiOperation("租户信息表-通过id查询")
    @GetMapping("/{id}")
    public Result<SsoTenant> queryById(@ApiParam(name = "id", value = "唯一性ID") @PathVariable String id) {
        SsoTenant ssoTenant = ssoTenantService.getById(id);
        return Result.ok(ssoTenant, "租户信息表-查询成功!");
    }


    /**
     * 导出
     *
     * @param reqSsoTenant 租户信息表请求参数
     * @param reqPage      分页参数
     * @throws IOException
     */
    @ApiOperation(value = "导出租户信息表", notes = "导出租户信息表")
    @GetMapping("/export")
    @RequiresPermissions("sys:ssoTenant:export")
    public void export(ReqSsoTenant reqSsoTenant, ReqPage reqPage) throws IOException {
        //swagger调用会用问题，使用postman测试
        ExcelUtils.write("SsoTenant", queryList(reqSsoTenant, reqPage));
    }

    /**
     * 租户切换
     *
     * @param tenantId 租户ID
     * @return
     */
    @ApiOperation("切换租户")
    @PutMapping("/change/{tenantId}")
    public Result<String> changeTenant(@PathVariable("tenantId") String tenantId) {
        if (StringUtils.isEmpty(tenantId)) {
            return Result.fail(tenantId, "错误:租户ID不允许为空");
        }
        List<TenantVo> list = OauthUtils.getTenants();
        if (list == null || list.stream().noneMatch((tenantVo -> tenantVo.getId().equals(tenantId)))) {
            return Result.fail(tenantId, "错误:该用户不属于此租户");
        }
        Object token = OauthUtils.getToken();
        if (token instanceof RedisAccessToken) {
            RedisAccessToken rat = (RedisAccessToken) token;
            if (tenantId.equals(rat.getTenantId())) {
                return Result.ok(tenantId, "切换租户成功");
            }
            rat.setTenantId(tenantId);
            OauthUtils.setToken(rat.getAccessToken(), rat);
            return Result.ok(tenantId, "切换租户成功");
        }
        if (token instanceof WeChatToken) {
            WeChatToken wct = (WeChatToken) token;
            if (tenantId.equals(wct.getTenantId())) {
                return Result.ok(tenantId, "切换租户成功");
            }
            wct.setTenantId(tenantId);
            OauthUtils.setToken(((WeChatToken) token).getAccess_token(), token);
            return Result.ok(tenantId, "切换租户成功");
        }
        return Result.fail(tenantId, "错误:未找到token");
    }

    /**
     * 查询租户组织树
     *
     * @param reqSsoOrg
     * @return
     */
    @ApiOperation(value = "获取租户组织树")
    @GetMapping("/org")
    public Result<List<SsoOrg>> queryOrgTree(ReqSsoOrg reqSsoOrg) {
        List<SsoOrg> list = ssoOrgService.queryOrg(reqSsoOrg.setTenantId(AuthInfoUtils.getCurrentTenantId()));
        List<SsoOrg> orgList = new ArrayList<>();
        TreeUtils.buildTree("", list, orgList, SsoOrg.class);
        return Result.ok(orgList, "组织结构表-查询成功!");
    }

    /**
     * 租户组织结构添加
     *
     * @param ssoOrg
     * @return
     */
    @Log(title = "租户组织结构-添加", operateType = OperateType.INSERT)
    @ApiOperation(value = "租户组织结构-添加", notes = "租户组织结构-添加")
    @PostMapping("/org")
    public Result<SsoOrg> orgAdd(@RequestBody SsoOrg ssoOrg) {
        Result<Boolean> result = verifyOrg(ssoOrg.getId());
        if (result.isSuccess()) {
            setParentOrg(ssoOrg);
            return ssoOrgService.insertOrg(ssoOrg);
        }
        return Result.fail(ssoOrg, result.getMsg());
    }

    /**
     * 租户组织结构编辑
     *
     * @param ssoOrg
     * @return
     */
    @Log(title = "租户组织结构-编辑", operateType = OperateType.UPDATE)
    @ApiOperation(value = "租户组织结构-编辑", notes = "租户组织结构-编辑")
    @PutMapping("/org")
    public Result<SsoOrg> orgEdit(@RequestBody SsoOrg ssoOrg) {
        Result<Boolean> result = verifyOrg(ssoOrg.getId());
        if (result.isSuccess()) {
            setParentOrg(ssoOrg);
            return ssoOrgService.updateOrg(ssoOrg);
        }
        return Result.fail(ssoOrg, result.getMsg());
    }

    /**
     * 租户是否管理员
     *
     * @return
     */
    private Result<Boolean> verifyTenant() {
        if (!ssoTenantService.isTenantMaster(AuthInfoUtils.getCurrentUserId(), AuthInfoUtils.getCurrentTenantId())) {
            return Result.fail(false, "错误:只允许管理员操作");
        }
        return Result.ok();
    }

    /**
     * 校验租户组织
     *
     * @param id 组织ID
     * @return
     */
    private Result<Boolean> verifyOrg(String id) {
        Result<Boolean> result = verifyTenant();
        if (!result.isSuccess()) {
            return result;
        }
        String tenantId = AuthInfoUtils.getCurrentTenantId();
        if (!StringUtils.isEmpty(id) && !ssoOrgService.isTenantOrg(id, tenantId)) {
            return Result.fail(false, "错误:不允许操作非自己租户下的组织");
        }
        return Result.ok();
    }

    /**
     * 设置父组织
     *
     * @param ssoOrg
     */
    private void setParentOrg(SsoOrg ssoOrg) {
        String tenantId = AuthInfoUtils.getCurrentTenantId();
        SsoOrg org = ssoOrgService.getBaseMapper().selectOne(new LambdaQueryWrapper<SsoOrg>().eq(SsoOrg::getTenantId, tenantId));
        if (org == null) {
            throw new MyRuntimeException("错误:未找到租户父组织");
        }
        if (StringUtils.isEmpty(ssoOrg.getParentId()) && !ssoOrg.getId().equals(org.getId())) {
            ssoOrg.setParentId(org.getId());
        }
    }


    /**
     * 通过id删除组织
     *
     * @param id
     * @return
     */
    @Log(title = "组织结构表-通过id删除", operateType = OperateType.DELETE)
    @ApiOperation(value = "组织结构表-通过id删除", notes = "组织结构表-通过id删除")
    @DeleteMapping("/org/{id}")
    public Result<Boolean> orgDelete(@ApiParam(name = "id", value = "唯一性ID") @PathVariable String id) {
        Result<Boolean> result = verifyOrg(id);
        if (result.isSuccess()) {
            return ssoOrgService.removeOrg(id);
        }
        return result;
    }

    /**
     * 查询租户角色列表
     *
     * @param reqSsoRole
     * @param reqPage
     * @return
     */
    @ApiOperation(value = "租户角色信息-分页列表查询", notes = "租户角色信息-分页列表查询")
    @GetMapping("/role")
    public Result<PageResult<SsoRole>> queryRolePageList(ReqSsoRole reqSsoRole, ReqPage reqPage) {
        PageHelper.startPage(reqPage.getPageNum(), reqPage.getPageSize());
        reqSsoRole.setTenantId(AuthInfoUtils.getCurrentTenantId());
        return Result.ok(new PageResult<>(ssoRoleService.list(SsoRoleController.buildCondition(reqSsoRole))), "角色信息表-查询成功!");
    }

    @ApiOperation(value = "角色信息表-列表查询", notes = "角色信息表-列表查询")
    @GetMapping("/role/all")
    public Result<List<SsoRole>> queryRoleList(ReqSsoRole reqSsoRole) {
        reqSsoRole.setTenantId(AuthInfoUtils.getCurrentTenantId());
        return Result.ok(ssoRoleService.list(SsoRoleController.buildCondition(reqSsoRole)), "角色信息表-查询成功!");
    }

    @ApiOperation("获取租户角色下的菜单ID")
    @GetMapping("/role/menus/{roleId}")
    public Result<List<String>> getRoleMenuIds(@ApiParam(name = "roleId", value = "角色ID") @PathVariable String roleId) {
        Result<Boolean> result = verifyRole(roleId);
        if (result.isSuccess()) {
            return Result.ok(ssoRoleService.getRoleMenus(roleId), "查询租户菜单成功");
        }
        return Result.fail(new ArrayList<>(), "错误:查询租户菜单ID失败");
    }

    @ApiOperation(value = "获取用户菜单树")
    @GetMapping("/menu/tree")
    public Result<List<SsoMenu>> queryMenuTree(ReqSsoMenu reqSsoMenu) {
        return ssoMenuService.queryMenuTree(reqSsoMenu, AuthInfoUtils.getCurrentUserId());
    }

    /**
     * 租户角色信息添加
     *
     * @param ssoRole
     * @return
     */
    @Log(title = "租户角色信息-添加", operateType = OperateType.INSERT)
    @ApiOperation(value = "角色信息表-添加", notes = "租户角色信息-添加")
    @PostMapping("/role")
    public Result<SsoRole> addRole(@RequestBody SsoRole ssoRole) {
        ssoRole.setTenantId(AuthInfoUtils.getCurrentTenantId());
        Result<Boolean> result = verifyRole(ssoRole.getId());
        if (result.isSuccess()) {
            return ssoRoleService.insertRole(ssoRole);
        }
        return Result.fail(ssoRole, result.getMsg());
    }

    /**
     * 租户角色信息编辑
     *
     * @param ssoRole
     * @return
     */
    @Log(title = "租户角色信息-编辑", operateType = OperateType.UPDATE)
    @ApiOperation(value = "角色信息表-编辑", notes = "租户角色信息-编辑")
    @PutMapping("/role")
    public Result<SsoRole> editRole(@RequestBody SsoRole ssoRole) {
        ssoRole.setTenantId(AuthInfoUtils.getCurrentTenantId());
        Result<Boolean> result = verifyRole(ssoRole.getId());
        if (result.isSuccess()) {
            return ssoRoleService.updateRole(ssoRole);
        }
        return Result.fail(ssoRole, result.getMsg());
    }

    @Log(title = "租户角色信息-通过id删除", operateType = OperateType.DELETE)
    @ApiOperation(value = "租户角色信息-通过id删除", notes = "租户角色信息-通过id删除")
    @DeleteMapping("/role/{id}")
    public Result<Boolean> deleteRole(@ApiParam(name = "id", value = "唯一性ID") @PathVariable String id) {
        Result<Boolean> result = verifyRole(id);
        if (result.isSuccess()) {
            return ssoRoleService.deleteRole(id);
        }
        return result;
    }

    @ApiOperation("获取租户列表-通过角色编码查询")
    @GetMapping("/roleCode/{roleCode}")
    public Result<List<TenantVo>> queryByRoleCode(@ApiParam(name = "roleCode", value = "角色编码") @PathVariable String roleCode) {
        return Result.ok(ssoTenantService.getTenantByRoleCode(roleCode), "获取租户列表成功!");
    }

    /**
     * 校验租户角色
     *
     * @param id
     * @return
     */
    private Result<Boolean> verifyRole(String id) {
        Result<Boolean> result = verifyTenant();
        if (!result.isSuccess()) {
            return result;
        }
        String tenantId = AuthInfoUtils.getCurrentTenantId();
        if (!StringUtils.isEmpty(id) && !ssoRoleService.isTenantRole(id, tenantId)) {
            return Result.fail(false, "错误:不允许操作非自己租户下的角色");
        }
        return Result.ok();
    }

    @ApiOperation(value = "租户用户信息-分页列表查询", notes = "租户用户信息-分页列表查询")
    @GetMapping("/user")
    public Result<PageResult<UserInfo>> queryUserPageList(ReqSsoUser reqSsoUser, ReqPage reqPage) {
        PageHelper.startPage(reqPage.getPageNum(), reqPage.getPageSize());
        reqSsoUser.setTenantId(AuthInfoUtils.getCurrentTenantId());
        List<UserInfo> pageList = ssoUserService.getUserList(reqSsoUser);
        return Result.ok(new PageResult<>(pageList), "租户人员信息-查询成功!");
    }

    @ApiOperation("用户组织关系绑定")
    @PostMapping("/user/org")
    public Result<Boolean> bindUserOrg(@RequestBody UserOrg userOrg) {
        Result<Boolean> result = verifyOrg(userOrg.getOrgId());
        if (!result.isSuccess()) {
            return result;
        }
        if (ssoUserService.isExistUserOrg(userOrg.getUserId(), userOrg.getOrgId())) {
            return Result.fail(false, "错误:用户已绑定该组织");
        }
        if (ssoUserService.insertUserOrg(userOrg.getUserId(), Collections.singletonList(userOrg.getOrgId())) > 0) {
            clearCache.removeUserCache(userOrg.getUserId());
            return Result.ok(true, "用户分配组织成功");
        }
        return Result.fail(false, "错误:用户分配组织失败");
    }

    @ApiOperation("用户组织关系移除")
    @DeleteMapping("/user/org")
    public Result<Boolean> deleteUserOrg(@RequestBody UserOrg userOrg) {
        Result<Boolean> result = verifyOrg(userOrg.getOrgId());
        if (!result.isSuccess()) {
            return result;
        }
        if (ssoTenantService.isTenantMasterOrg(userOrg.getUserId(), userOrg.getOrgId())) {
            return Result.fail(false, "错误:不允许移除租户管理员");
        }
        if (ssoUserService.deleteUserOrg(userOrg.getUserId(), userOrg.getOrgId()) > 0) {
            clearCache.removeUserCache(userOrg.getUserId());
            return Result.ok(true, "用户移出组织成功");
        }
        return Result.fail(false, "错误:用户移出组织失败");
    }
}
