package com.ssx.sandbox.wechat.controller;

import com.alibaba.fastjson2.JSONObject;
import com.github.binarywang.wxpay.bean.request.BaseWxPayRequest;
import com.github.binarywang.wxpay.bean.result.BaseWxPayResult;
import com.github.binarywang.wxpay.util.SignUtils;
import com.ssx.sandbox.wechat.conf.WechatPayProperties;
import com.ssx.sandbox.wechat.util.DateUtils;
import com.thoughtworks.xstream.XStream;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.xml.XStreamInitializer;
import okhttp3.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Brook
 * @version 1.0.0
 */
@Component
@Slf4j
public class BaseController {
    protected static final String SUCCESS = "SUCCESS";
    protected static final String USERPAYING = "USERPAYING";
    protected static final String FAIL = "FAIL";

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().build();
    private static final ScheduledExecutorService pool = new ScheduledThreadPoolExecutor(2);

    @Resource
    private WechatPayProperties wechatPayProperties;

    public <T extends BaseWxPayResult> T successResult(Class<T> clazz) {
        try {
            T result = clazz.newInstance();

            result.setReturnCode(SUCCESS);
            result.setResultCode(SUCCESS);

            result.setAppid(wechatPayProperties.getAppId());
            result.setMchId(wechatPayProperties.getMchId());

            result.setNonceStr(RandomStringUtils.random(32, true, true));
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends BaseWxPayResult> T failResult(Class<T> clazz, String errorCode, String errorMsg) {
        try {
            T result = clazz.newInstance();

            result.setReturnCode(SUCCESS);
            result.setResultCode(FAIL);
            // result.setErrorCode("NOT_FOUND");
            // result.setErrCodeDes("未找到该记录");
            result.setErrorCode(errorCode);
            result.setErrCodeDes(errorMsg);

            result.setAppid(wechatPayProperties.getAppId());
            result.setMchId(wechatPayProperties.getMchId());

            result.setNonceStr(RandomStringUtils.random(32, true, true));
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void copyProperties(Object source, Object target) {
        BeanUtils.copyProperties(source, target);
    }

    public String signAndToXml(BaseWxPayResult result) {
        sign(result);
        return toXML(result);
    }

    public void sign(BaseWxPayResult result) {
        result.setXmlString(toXML(result));
        result.setSign(SignUtils.createSign(result.toMap(), null, wechatPayProperties.getMchKey(), new String[]{"xmlString"}));
        result.setXmlString(null);
        result.setXmlDoc(null);
    }

    public static <T extends BaseWxPayRequest> T fromXml(String xml, Class<T> clazz) {
        XStream xstream = XStreamInitializer.getInstance();
        xstream.processAnnotations(clazz);
        xstream.setClassLoader(BaseWxPayRequest.class.getClassLoader());
        return (T) xstream.fromXML(xml);
    }

    public static String toXML(BaseWxPayResult result) {
        // 涉及到服务商模式的两个参数，在为空值时置为null，以免在请求时将空值传给微信服务器
        result.setSubAppId(StringUtils.trimToNull(result.getSubAppId()));
        result.setSubMchId(StringUtils.trimToNull(result.getSubMchId()));
        XStream xstream = XStreamInitializer.getInstance();
        xstream.processAnnotations(result.getClass());
        return xstream.toXML(result);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return JSONObject.parseObject(json, clazz);
    }

    public static String randomOpenId() {
        return randomString(16);
    }

    public static String randomString(int count) {
        return "sandbox" + RandomStringUtils.random(count - 7, false, true);
    }

    public static String now() {
        return DateUtils.format(System.currentTimeMillis(), DateUtils.FMT_yyyyMMddHHmmss14);
    }

    /**
     * 延时运行
     *
     * @param payResult
     * @param url
     * @param delay
     * @param timeUnit
     */
    public void delayRequest(BaseWxPayResult payResult, String url, long delay, TimeUnit timeUnit) {
        Runnable runnable = () -> {
            String xml = signAndToXml(payResult);
            log.info("----------> url: {}, \nxml: {}", url, xml);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(MediaType.parse("application/xml"), xml))
                    .build();
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (response.body() != null) {
                    log.info("<---------- response: {}, body: {}", response, response.body().string());
                } else {
                    log.info("<---------- response: {}", response);
                }
            } catch (IOException e) {
                log.error("http request error", e);
            }
        };

        pool.schedule(runnable, delay, timeUnit);
    }
}
