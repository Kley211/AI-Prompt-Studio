package org.dromara.ai.common.error;

/**
 * AI 平台稳定错误码。
 */
public enum AiErrorCode {

    INVALID_REQUEST(400, "AI-400-001", "请求参数无效"),
    PROJECT_ACCESS_DENIED(403, "AI-403-001", "无权访问该项目资源"),
    RESOURCE_NOT_FOUND(404, "AI-404-001", "资源不存在"),
    CONFLICT(409, "AI-409-001", "资源状态冲突"),
    INTERNAL_ERROR(500, "AI-500-001", "AI 平台内部错误");

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    AiErrorCode(int httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
