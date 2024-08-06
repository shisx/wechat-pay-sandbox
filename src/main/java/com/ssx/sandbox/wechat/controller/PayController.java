package com.ssx.sandbox.wechat.controller;

import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.request.*;
import com.github.binarywang.wxpay.bean.result.*;
import com.ssx.sandbox.wechat.conf.WechatPayProperties;
import com.ssx.sandbox.wechat.db.MemoryDB;
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

    /**
     * 统一支付
     *
     * @param xml
     * @return
     */
    @PostMapping(path = "/pay/unifiedorder")
    public String unifiedOrder(@RequestBody String xml) {
        WxPayUnifiedOrderRequest request = fromXml(xml, WxPayUnifiedOrderRequest.class);

        // 响应
        WxPayUnifiedOrderResult result = successResult(WxPayUnifiedOrderResult.class);
        copyProperties(request, result);
        result.setPrepayId(randomString(32));
        sign(result);

        // 支付结果通知
        WxPayOrderNotifyResult notifyResult = successResult(WxPayOrderNotifyResult.class);
        copyProperties(request, notifyResult);
        notifyResult.setIsSubscribe("N");
        notifyResult.setBankType("CMC");
        notifyResult.setCashFee(0);
        notifyResult.setTransactionId(randomString(32));
        notifyResult.setTimeEnd(now());
        notifyResult.setOpenid(randomOpenId());
        sign(notifyResult);
        delayRequest(notifyResult, request.getNotifyUrl(), 5, TimeUnit.SECONDS);

        MemoryDB.put(request);
        MemoryDB.put(request.getOutTradeNo(), result);
        MemoryDB.put(notifyResult);

        return toXML(result);
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

        MemoryDB.Tables tables = MemoryDB.get(request.getContractCode());
        if (tables.getWxSignStatusNotifyResult() == null) {
            WxPayEntrustResult result = failResult(WxPayEntrustResult.class, "HAS_ENTRUST_REQUEST", "签约记录已经存在，请勿重复请求");
            return signAndToXml(result);
        }

        // 响应
        WxPayEntrustResult result = successResult(WxPayEntrustResult.class);
        copyProperties(request, result);
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

        MemoryDB.put(request);
        MemoryDB.put(result);
        MemoryDB.put(notifyResult);

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

        WxPayOrderNotifyResult notifyResult = MemoryDB.get(request.getOutTradeNo(), request.getTransactionId()).getWxPayOrderNotifyResult();

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
    @PostMapping(path = "/v3/papay/contracts/%s/notify")
    public String contractsNotify(@PathVariable String contractId, @RequestBody String json) {
        WxPreWithholdRequest request = fromJson(json, WxPreWithholdRequest.class);
        request.setContractId(contractId);
        MemoryDB.put(request);
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
        WxWithholdRequest request = fromJson(xml, WxWithholdRequest.class);

        MemoryDB.Tables tables = MemoryDB.get(request.getContractId());
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

        MemoryDB.put(request);
        MemoryDB.put(notifyResult);

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

        MemoryDB.Tables tables = MemoryDB.get(request.getContractId());
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
        signResult.setOpenId(randomOpenId());
        signResult.setChangeType("DELETE");
        signResult.setOperateTime(DateUtils.format(LocalDateTime.now(), DateUtils.FMT_yyyyMMddHHmmss19));
        delayRequest(signResult, wechatPayProperties.getContractNotifyUrl(), 10, TimeUnit.SECONDS);

        MemoryDB.put(request);
        MemoryDB.put(signResult);

        return signAndToXml(result);
    }


}
