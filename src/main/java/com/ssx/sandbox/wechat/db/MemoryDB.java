package com.ssx.sandbox.wechat.db;

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
 * 内存数据（不考虑内存溢出）
 *
 * @author Brook
 * @version 2.0.0
 */
public class MemoryDB {

    // 请求数据，key为outTradeNo
    private static final Map<String, Tables> dataMap = new HashMap<>();
    // Key之间映射
    private static final Map<String, String> keyMap = new HashMap<>();

    private static final Tables EMPTY = new Tables();

    public static void put(WxPayUnifiedOrderRequest table) {
        setting(table.getOutTradeNo(), table);
    }

    public static void put(String outTradeNo, WxPayUnifiedOrderResult table) {
        setting(outTradeNo, table);
    }

    public static void put(WxPayOrderNotifyResult table) {
        setting(table.getOutTradeNo(), table);
        keyMap.put(table.getTransactionId(), table.getOutTradeNo());
    }

    public static void put(WxPayEntrustRequest table) {
        setting(table.getOutTradeNo(), table);
        keyMap.put(table.getContractCode(), table.getOutTradeNo());
    }

    public static void put(WxPayEntrustResult table) {
        setting(table.getOutTradeNo(), table);
        keyMap.put(table.getContractCode(), table.getOutTradeNo());
    }

    public static void put(WxSignStatusNotifyResult table) {
        String outTradeNo = keyMap.get(table.getContractCode());
        if (outTradeNo != null) {
            setting(outTradeNo, table);
            keyMap.put(table.getContractId(), outTradeNo);
        }
    }

    public static void put(WxPreWithholdRequest table) {
        String outTradeNo = keyMap.get(table.getContractId());
        if (outTradeNo != null) {
            setting(outTradeNo, table);
        }
    }

    public static void put(WxWithholdRequest table) {
        setting(table.getOutTradeNo(), table);
    }

    public static void put(WxWithholdNotifyResult table) {
        setting(table.getOutTradeNo(), table);
        keyMap.put(table.getTransactionId(), table.getOutTradeNo());
        keyMap.put(table.getContractId(), table.getOutTradeNo());
    }

    public static void put(WxTerminatedContractRequest table) {
        String outTradeNo = keyMap.get(table.getContractId());
        if (outTradeNo != null) {
            setting(outTradeNo, table);
        }
    }

    private static void setting(String key, Object val) {
        if (key == null) {
            return;
        }
        dataMap.compute(key, (k, v) -> {
            if (v == null) {
                v = new Tables();
            }
            v.setting(val);
            return v;
        });
    }

    public static Tables get(String... key) {
        Tables tables;
        for (String s : key) {
            tables = dataMap.get(s);
            if (tables != null) {
                return tables;
            }
        }
        return EMPTY;
    }

    @Data
    public static final class Tables {
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

        private Map<Class<?>, LocalDateTime> timeMap = new HashMap<>();

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

            timeMap.put(obj.getClass(), LocalDateTime.now());
        }

        public LocalDateTime getTime(Class<?> clazz) {
            return timeMap.get(clazz);
        }
    }
}
