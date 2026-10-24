package com.hundunlab.site.common.constant;

/**
 * 审计日志的操作模块标识。
 * 只保留当前真实存在的模块，新增模块时在此追加，避免出现"看起来有、其实没有"的模块名。
 */
public final class ModuleNameConstant {

    private ModuleNameConstant() {
    }

    public static final String USER = "用户模块";
    public static final String ADMIN = "管理员模块";
    public static final String FEEDBACK = "反馈模块";
    public static final String SOFTWARE = "软件模块";
    public static final String PLUGIN = "插件模块";
}
