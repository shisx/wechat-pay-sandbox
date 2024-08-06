package com.ssx.sandbox.wechat.controller.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/**
 * 异常拦截与处理
 */
@RestControllerAdvice
@Slf4j
public class ExceptionHandler {

    public static final String WECHAT_SUCCESS_RESPONSE =
            "<xml>\n" +
                    "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                    "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                    "</xml>";
    public static final String WECHAT_FAIL_RESPONSE =
            "<xml>\n" +
                    "  <return_code><![CDATA[FAIL]]></return_code>\n" +
                    "  <return_msg><![CDATA[%s]]></return_msg>\n" +
                    "</xml>";

    @org.springframework.web.bind.annotation.ExceptionHandler(value = Exception.class)
    public String exception(Exception e) {
        log.debug("exception {}", e.getMessage());
        return String.format(WECHAT_FAIL_RESPONSE, "系统异常");
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(value = SQLException.class)
    public String sqlException(SQLException e) {
        log.error("sqlException", e);
        return String.format(WECHAT_FAIL_RESPONSE, "SQL错误");
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(value = JsonProcessingException.class)
    public String jsonProcessingException(JsonProcessingException e) {
        log.error("jsonProcessingException", e);
        return String.format(WECHAT_FAIL_RESPONSE, "请求消息体格式错误");
    }

}
