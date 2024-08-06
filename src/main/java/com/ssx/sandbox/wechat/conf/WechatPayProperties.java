package com.ssx.sandbox.wechat.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信支付相关参数
 *
 * @author Brook
 * @version 4.1.0
 */
@Data
@ConfigurationProperties("wechat.pay")
public class WechatPayProperties {

    /**
     * 支付应用appId
     */
    private String appId;

    /**
     * 商户ID
     */
    private String mchId;

    /**
     * 商户密钥
     */
    private String mchKey;

    /**
     * 微信证书路径
     */
    private String keyPath;

    /**
     * 通知回调地址
     */
    private String notifyUrl;

    /**
     * 退款回调地址
     */
    private String refundNotifyUrl;

    /**
     * 签约回调地址
     */
    private String contractNotifyUrl;

}
