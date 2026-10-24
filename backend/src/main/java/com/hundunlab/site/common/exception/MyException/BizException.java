package com.hundunlab.site.common.exception.MyException;

import com.hundunlab.site.common.response.ErrorCode;
import lombok.Getter;

/**
 * 带业务错误码的异常。
 * <p>
 * 新模块统一抛这个，而不是散落的字符串消息：前端只认 {@link ErrorCode}，
 * 自己查 i18n 文案，后端返回什么语言都不影响切换后的显示。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }
}
