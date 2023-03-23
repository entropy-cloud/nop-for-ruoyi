package com.ruoyi.framework.web.service;

import io.nop.api.core.auth.IActionAuthChecker;
import io.nop.api.core.auth.IUserContext;
import io.nop.commons.util.StringHelper;

import javax.inject.Inject;

public class NopActionAuthChecker implements IActionAuthChecker {

    @Inject
    PermissionService permissionService;

    @Override
    public boolean isPermitted(String permission, IUserContext iUserContext) {
        boolean b = permissionService.hasPermi(permission);
        // 假定写权限总是隐含读权限
        if (!b && permission.endsWith(":query")) {
            String prefix = StringHelper.removeTail(permission, ":query");
            b = permissionService.hasPermi(prefix + ":mutation");
        }
        return b;
    }
}