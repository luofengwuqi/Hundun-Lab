package com.hundunlab.site.common.constant.email;

/**
 * 邮件文案常量。
 * 阶段 1 只收敛主题与收件人逻辑；正式的双语文案在阶段 3 随通知模块一起改为模板 + 文案键。
 */
public final class TEXT {

    private TEXT() {
    }

    public static final String EMAIL_CODE_SUBJECT = "【Hundun Lab】邮箱验证码";

    public static final String EMAIL_CODE_CONTENT_PREFIX = "您的验证码是：";

    public static final String FEEDBACK_ADDED_SUBJECT = "【Hundun Lab】收到新的反馈";

    public static final String FEEDBACK_FINISHED_SUBJECT = "【Hundun Lab】反馈已处理完成";

    public static final String FEEDBACK_BANED_SUBJECT = "【Hundun Lab】反馈未通过";

    public static final String FEEDBACK_ADDED_MSG_TO_ADMIN = """
            Hundun Lab 收到一条新的用户反馈。

            请登录管理后台查看并处理。""";

    public static final String FEEDBACK_FINISHED_MSG_TO_USER = """
            Hundun Lab 你好：

            你提交的反馈已处理完成，感谢你的反馈。""";

    public static final String FEEDBACK_BANED_MSG_TO_USER = """
            Hundun Lab 你好：

            很抱歉，你提交的反馈未通过审核。
            如仍有疑问，可再次提交反馈说明情况。""";
}
