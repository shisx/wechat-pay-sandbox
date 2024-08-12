package com.ssx.sandbox.wechat.controller;

import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.request.*;
import com.github.binarywang.wxpay.bean.result.*;
import com.ssx.sandbox.wechat.conf.WechatPayProperties;
import com.ssx.sandbox.wechat.data.MemoryDB;
import com.ssx.sandbox.wechat.data.Tables;
import com.ssx.sandbox.wechat.util.DateUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 微信支付接口
 */
@RestController
public class PayController extends BaseController {

    @Resource
    private WechatPayProperties wechatPayProperties;
    @Resource
    private MemoryDB memoryDB;

    /**
     * 统一支付
     *
     * @param xml
     * @return
     */
    @PostMapping(path = "/pay/unifiedorder")
    public String unifiedOrder(@RequestBody String xml) {
        WxPayUnifiedOrderRequest request = fromXml(xml, WxPayUnifiedOrderRequest.class);

        Tables tables = memoryDB.get(request.getOutTradeNo());
        if (tables != null && tables.getWxPayUnifiedOrderRequest() != null) {
            WxPayUnifiedOrderResult result = failResult(WxPayUnifiedOrderResult.class, "REPEAT_OUT_TRADE_NO", "重复的商户订单号");
            copyProperties(request, result);
            return signAndToXml(result);
        }

        // 响应
        WxPayUnifiedOrderResult result = successResult(WxPayUnifiedOrderResult.class);
        copyProperties(request, result);
        result.setPrepayId(randomString(32));

        // 支付结果通知
        WxPayOrderNotifyResult notifyResult = successResult(WxPayOrderNotifyResult.class);
        copyProperties(request, notifyResult);
        notifyResult.setIsSubscribe("N");
        notifyResult.setBankType("CMC");
        notifyResult.setCashFee(0);
        notifyResult.setTransactionId(randomString(32));
        notifyResult.setTimeEnd(now());
        notifyResult.setOpenid(randomOpenId());
        delayRequest(notifyResult, request.getNotifyUrl(), 5, TimeUnit.SECONDS);

        memoryDB.put(request);
        memoryDB.put(request.getOutTradeNo(), result);
        memoryDB.put(notifyResult);

        return signAndToXml(result);
    }

    /**
     * 支付中签约
     *
     * @param xml
     * @return
     */
    @PostMapping(path = "/pay/contractorder")
    public String payContractOrder(@RequestBody String xml) {
        WxPayEntrustRequest request = fromXml(xml, WxPayEntrustRequest.class);

        Tables tables = memoryDB.get(request.getContractCode(), request.getOutTradeNo());
        if (tables.getWxSignStatusNotifyResult() != null) {
            WxPayEntrustResult result = failResult(WxPayEntrustResult.class, "HAS_ENTRUST_REQUEST", "签约记录已经存在，请勿重复请求");
            return signAndToXml(result);
        }

        // 响应
        WxPayEntrustResult result = successResult(WxPayEntrustResult.class);
        copyProperties(request, result);
        result.setContractResultCode(SUCCESS);
        result.setPrepayId(randomString(32));

        String openId = Optional.ofNullable(request.getOpenId()).orElse(randomOpenId());

        // 支付结果通知 ~5s
        WxPayOrderNotifyResult orderResult = successResult(WxPayOrderNotifyResult.class);
        copyProperties(request, orderResult);
        orderResult.setIsSubscribe("N");
        orderResult.setBankType("CMC");
        orderResult.setCashFee(0);
        orderResult.setTransactionId(randomString(32));
        orderResult.setTimeEnd(now());
        orderResult.setOpenid(openId);
        delayRequest(orderResult, request.getNotifyUrl(), 5, TimeUnit.SECONDS);

        // 签约结果通知 ~10s
        WxSignStatusNotifyResult notifyResult = successResult(WxSignStatusNotifyResult.class);
        copyProperties(request, notifyResult);
        notifyResult.setOpenId(openId);
        notifyResult.setChangeType("ADD");
        notifyResult.setOperateTime(DateUtils.format(LocalDateTime.now(), DateUtils.FMT_yyyyMMddHHmmss19));
        notifyResult.setContractId(randomString(16));
        delayRequest(notifyResult, request.getContractNotifyUrl(), 10, TimeUnit.SECONDS);

        memoryDB.put(request);
        memoryDB.put(result);
        memoryDB.put(orderResult);
        memoryDB.put(notifyResult);

        return signAndToXml(result);
    }

    /**
     * 订单查询
     *
     * @param xml
     * @return
     */
    @PostMapping(path = "/pay/orderquery")
    public String payOrderQuery(@RequestBody String xml) {
        WxPayOrderQueryRequest request = fromXml(xml, WxPayOrderQueryRequest.class);

        WxPayOrderNotifyResult notifyResult = memoryDB.get(request.getOutTradeNo(), request.getTransactionId()).getWxPayOrderNotifyResult();

        WxPayOrderQueryResult result;
        if (notifyResult != null) {
            // 响应
            result = successResult(WxPayOrderQueryResult.class);
            copyProperties(notifyResult, result);
            result.setTradeStateDesc("交易成功了哦");
        } else {
            result = failResult(WxPayOrderQueryResult.class, "NOT_FOUND", "未找到该记录");
        }

        return signAndToXml(result);
    }

    /**
     * 预扣费通知
     *
     * @param contractId
     * @param json
     * @return
     */
    @PostMapping(path = "/v3/papay/contracts/{contractId}/notify")
    public String contractsNotify(@PathVariable String contractId, @RequestBody String json) {
        WxPreWithholdRequest request = fromJson(json, WxPreWithholdRequest.class);

        Tables tables = memoryDB.get(contractId);
        if (tables.getWxPayEntrustResult() == null) {
            WxWithholdResult result = failResult(WxWithholdResult.class, "NO_ENTRUST_ORDER", "没有找到签约记录");
            return signAndToXml(result);
        }
        if (tables.getWxSignStatusNotifyResult() != null && "DELETE".equals(tables.getWxSignStatusNotifyResult().getChangeType())) {
            WxWithholdResult result = failResult(WxWithholdResult.class, "HAS_DELETED", "已经解约，不能进行该操作");
            return signAndToXml(result);
        }
        if (tables.getWxPreWithholdRequest() != null) {
            WxWithholdResult result = failResult(WxWithholdResult.class, "HAS_NOTIFY", "已经发送过通知");
            return signAndToXml(result);
        }

        request.setContractId(contractId);
        memoryDB.put(request);
        return "{}";
    }

    /**
     * 申请扣费
     *
     * @param xml
     * @return
     */
    @PostMapping(path = "/pay/pappayapply")
    public String papPayApply(@RequestBody String xml) {
        WxWithholdRequest request = fromXml(xml, WxWithholdRequest.class);

        Tables tables = memoryDB.get(request.getContractId());
        if (tables == null || tables.getWxPayEntrustResult() == null) {
            WxWithholdResult result = failResult(WxWithholdResult.class, "NO_ENTRUST_ORDER", "没有找到签约记录");
            return signAndToXml(result);
        }
        if (tables.getWxSignStatusNotifyResult() != null && "DELETE".equals(tables.getWxSignStatusNotifyResult().getChangeType())) {
            WxWithholdResult result = failResult(WxWithholdResult.class, "HAS_DELETED", "已经解约，不能进行该操作");
            return signAndToXml(result);
        }
        if (tables.getWxPreWithholdRequest() == null) {
            WxWithholdResult result = failResult(WxWithholdResult.class, "HAS_NOT_NOTIFY", "还未发送扣费前通知");
            return signAndToXml(result);
        } else {
            LocalDateTime time = tables.getTime(WxPreWithholdRequest.class);
            if (time.toLocalDate().plusDays(3).isBefore(LocalDate.now())) {
                WxWithholdResult result = failResult(WxWithholdResult.class, "NOTIFY_TIME_LIMIT", "扣费等待期还未结束");
                return signAndToXml(result);
            }
        }
        if (tables.getWxWithholdRequest() != null) {
            WxWithholdResult result = failResult(WxWithholdResult.class, "HAS_PAP_PAY_APPLY", "已经发送过扣费申请了");
            return signAndToXml(result);
        }

        WxWithholdResult result = successResult(WxWithholdResult.class);
        copyProperties(request, result);

        // 扣费结果回调
        WxWithholdNotifyResult notifyResult = successResult(WxWithholdNotifyResult.class);
        copyProperties(request, notifyResult);
        notifyResult.setOpenId(randomOpenId());
        notifyResult.setBankType("CMC");
        notifyResult.setCashFee(0);
        notifyResult.setTradeState("SUCCESS");
        notifyResult.setTransactionId(randomString(32));
        notifyResult.setTimeEnd(now());
        delayRequest(notifyResult, request.getNotifyUrl(), 10, TimeUnit.SECONDS);

        memoryDB.put(request);
        memoryDB.put(notifyResult);

        return signAndToXml(result);
    }

    /**
     * 查询签约关系
     *
     * @param xml
     * @return
     */
    @PostMapping(path = "/papay/querycontract")
    public String queryContract(@RequestBody String xml) {
        WxSignQueryRequest request = fromXml(xml, WxSignQueryRequest.class);

        Tables tables = memoryDB.get(request.getContractId());
        WxPayEntrustResult entrustResult = tables.getWxPayEntrustResult();
        WxSignStatusNotifyResult signStatusNotifyResult = tables.getWxSignStatusNotifyResult();
        WxTerminatedContractRequest terminatedContractRequest = tables.getWxTerminatedContractRequest();
        if (entrustResult == null) {
            WxSignQueryResult result = failResult(WxSignQueryResult.class, "NO_ENTRUST_ORDER", "没有找到签约记录");
            return signAndToXml(result);
        }

        WxSignQueryResult result = successResult(WxSignQueryResult.class);
        copyProperties(request, result);
        result.setContractCode(entrustResult.getContractCode());
        result.setContractSignedTime(DateUtils.format(tables.getTime(WxPayEntrustResult.class), DateUtils.FMT_yyyyMMddHHmmss19));
        if (signStatusNotifyResult != null) {
            result.setContractId(signStatusNotifyResult.getContractId());
            result.setOpenId(signStatusNotifyResult.getOpenId());
            if ("ADD".equals(signStatusNotifyResult.getChangeType())) {
                result.setContractState(0);
            } else if ("DELETE".equals(signStatusNotifyResult.getChangeType())) {
                result.setContractTerminatedTime(DateUtils.format(tables.getTime(WxTerminatedContractRequest.class), DateUtils.FMT_yyyyMMddHHmmss19));
                result.setContractTerminationRemark(terminatedContractRequest.getContractTerminationRemark());
                result.setContractState(1);
            } else {
                result.setContractState(9);
            }
        }
        result.setContractExpiredTime("2099-01-01 00:00:00");

        return signAndToXml(result);
    }

    /**
     * 申请解约
     *
     * @param xml
     * @return
     */
    @PostMapping(path = "/papay/deletecontract")
    public String deleteContract(@RequestBody String xml) {
        WxTerminatedContractRequest request = fromXml(xml, WxTerminatedContractRequest.class);

        Tables tables = memoryDB.get(request.getContractId());
        if (tables.getWxPayEntrustResult() == null) {
            WxTerminationContractResult result = failResult(WxTerminationContractResult.class, "NO_ENTRUST_ORDER", "没有找到签约记录");
            return signAndToXml(result);
        }
        if (tables.getWxSignStatusNotifyResult() != null && "DELETE".equals(tables.getWxSignStatusNotifyResult().getChangeType())) {
            WxTerminationContractResult result = failResult(WxTerminationContractResult.class, "HAS_DELETED", "已经解约，请勿重复请求");
            return signAndToXml(result);
        }

        WxTerminationContractResult result = successResult(WxTerminationContractResult.class);
        copyProperties(request, result);

        // 解约结果回调
        WxSignStatusNotifyResult signResult = successResult(WxSignStatusNotifyResult.class);
        copyProperties(request, signResult);
        signResult.setContractCode(tables.getWxPayEntrustResult().getContractCode());
        signResult.setOpenId(tables.getWxSignStatusNotifyResult().getOpenId());
        signResult.setChangeType("DELETE");
        signResult.setContractTerminationMode(2);
        signResult.setOperateTime(DateUtils.format(LocalDateTime.now(), DateUtils.FMT_yyyyMMddHHmmss19));
        delayRequest(signResult, wechatPayProperties.getContractNotifyUrl(), 10, TimeUnit.SECONDS);

        memoryDB.put(request);
        memoryDB.put(signResult);

        return signAndToXml(result);
    }

}
