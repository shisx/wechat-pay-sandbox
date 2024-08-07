package com.ssx.sandbox.wechat.data;

import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.request.*;
import com.github.binarywang.wxpay.bean.result.WxPayEntrustResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.bean.result.WxSignStatusNotifyResult;
import com.github.binarywang.wxpay.bean.result.WxWithholdNotifyResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Brook
 * @version 1.0.0
 */
@Data
public final class Tables {

    public static final Tables EMPTY = new Tables();

    private WxPayUnifiedOrderRequest wxPayUnifiedOrderRequest;
    private WxPayUnifiedOrderResult wxPayUnifiedOrderResult;
    private WxPayOrderNotifyResult wxPayOrderNotifyResult;
    private WxPayEntrustRequest wxPayEntrustRequest;
    private WxPayEntrustResult wxPayEntrustResult;
    private WxSignStatusNotifyResult wxSignStatusNotifyResult;
    private WxPreWithholdRequest wxPreWithholdRequest;
    private WxWithholdRequest wxWithholdRequest;
    private WxWithholdNotifyResult wxWithholdNotifyResult;
    private WxTerminatedContractRequest wxTerminatedContractRequest;

    private Map<String, LocalDateTime> timeMap = new HashMap<>();

    public void setting(Object obj) {
        if (obj instanceof WxPayUnifiedOrderRequest) {
            wxPayUnifiedOrderRequest = (WxPayUnifiedOrderRequest) obj;
        } else if (obj instanceof WxPayUnifiedOrderResult) {
            wxPayUnifiedOrderResult = (WxPayUnifiedOrderResult) obj;
        } else if (obj instanceof WxPayOrderNotifyResult) {
            wxPayOrderNotifyResult = (WxPayOrderNotifyResult) obj;
        } else if (obj instanceof WxPayEntrustRequest) {
            wxPayEntrustRequest = (WxPayEntrustRequest) obj;
        } else if (obj instanceof WxPayEntrustResult) {
            wxPayEntrustResult = (WxPayEntrustResult) obj;
        } else if (obj instanceof WxSignStatusNotifyResult) {
            wxSignStatusNotifyResult = (WxSignStatusNotifyResult) obj;
        } else if (obj instanceof WxPreWithholdRequest) {
            wxPreWithholdRequest = (WxPreWithholdRequest) obj;
        } else if (obj instanceof WxWithholdRequest) {
            wxWithholdRequest = (WxWithholdRequest) obj;
        } else if (obj instanceof WxWithholdNotifyResult) {
            wxWithholdNotifyResult = (WxWithholdNotifyResult) obj;
        } else if (obj instanceof WxTerminatedContractRequest) {
            wxTerminatedContractRequest = (WxTerminatedContractRequest) obj;
        } else {
            return;
        }

        timeMap.put(obj.getClass().getSimpleName(), LocalDateTime.now());
    }

    public LocalDateTime getTime(Class<?> clazz) {
        return timeMap.get(clazz.getSimpleName());
    }

    public LocalDateTime getTime(Object obj) {
        return timeMap.get(obj.getClass().getSimpleName());
    }
}
