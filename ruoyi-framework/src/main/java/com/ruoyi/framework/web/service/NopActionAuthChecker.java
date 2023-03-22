package com.ruoyi.framework.web.service;

import io.nop.api.core.auth.IActionAuthChecker;
import io.nop.api.core.auth.IUserContext;

import javax.inject.Inject;

public class NopActionAuthChecker implements IActionAuthChecker {

    @Inject
    PermissionService permissionService;

    @Override
    public boolean isPermitted(String permission, IUserContext iUserContext) {
        return permissionService.hasPermi(permission);
    }
}