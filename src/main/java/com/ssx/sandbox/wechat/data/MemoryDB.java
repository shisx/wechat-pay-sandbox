package com.ssx.sandbox.wechat.data;

import com.github.binarywang.wxpay.bean.notify.WxPayOrderNotifyResult;
import com.github.binarywang.wxpay.bean.request.*;
import com.github.binarywang.wxpay.bean.result.WxPayEntrustResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.bean.result.WxSignStatusNotifyResult;
import com.github.binarywang.wxpay.bean.result.WxWithholdNotifyResult;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 内存数据（不考虑内存溢出）
 *
 * @author Brook
 * @version 1.0.0
 */
@Component
public class MemoryDB {

    // 请求数据，key为outTradeNo
    private final Map<String, Tables> dataMap = new HashMap<>();
    // Key之间映射
    private final Map<String, String> keyMap = new HashMap<>();

    private long lastTime = System.currentTimeMillis();

    public Map<String, Tables> getDataMap() {
        return Collections.unmodifiableMap(dataMap);
    }

    public Map<String, String> getKeyMap() {
        return Collections.unmodifiableMap(keyMap);
    }

    public long getLastTime() {
        return lastTime;
    }

    public void putMap(Map<String, Tables> dataMap, Map<String, String> keyMap) {
        this.dataMap.putAll(dataMap);
        this.keyMap.putAll(keyMap);
        lastTime = System.currentTimeMillis();
    }

    public void put(WxPayUnifiedOrderRequest table) {
        setting(table.getOutTradeNo(), table);
    }

    public void put(String outTradeNo, WxPayUnifiedOrderResult table) {
        setting(outTradeNo, table);
    }

    public void put(WxPayOrderNotifyResult table) {
        setting(table.getOutTradeNo(), table);
        keyMap.put(table.getTransactionId(), table.getOutTradeNo());
    }

    public void put(WxPayEntrustRequest table) {
        setting(table.getOutTradeNo(), table);
        keyMap.put(table.getContractCode(), table.getOutTradeNo());
    }

    public void put(WxPayEntrustResult table) {
        setting(table.getOutTradeNo(), table);
        keyMap.put(table.getContractCode(), table.getOutTradeNo());
    }

    public void put(WxSignStatusNotifyResult table) {
        String outTradeNo = keyMap.get(table.getContractCode());
        if (outTradeNo != null) {
            setting(outTradeNo, table);
            keyMap.put(table.getContractId(), outTradeNo);
        }
    }

    public void put(WxPreWithholdRequest table) {
        String outTradeNo = keyMap.get(table.getContractId());
        if (outTradeNo != null) {
            setting(outTradeNo, table);
        }
    }

    public void put(WxWithholdRequest table) {
        setting(table.getOutTradeNo(), table);
    }

    public void put(WxWithholdNotifyResult table) {
        setting(table.getOutTradeNo(), table);
        keyMap.put(table.getTransactionId(), table.getOutTradeNo());
        // keyMap.put(table.getContractId(), table.getOutTradeNo());// 由于contractId相同，会覆盖原有数据，这里保存
    }

    public void put(WxTerminatedContractRequest table) {
        String outTradeNo = keyMap.get(table.getContractId());
        if (outTradeNo != null) {
            setting(outTradeNo, table);
        }
    }

    private void setting(String key, Object val) {
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
        lastTime = System.currentTimeMillis();
    }

    public Tables get(String... key) {
        Tables tables;
        for (String s : key) {
            if (s == null) {
                continue;
            }
            tables = dataMap.get(s);
            if (tables != null) {
                return tables;
            }
        }

        String outTradeNo;
        for (String s : key) {
            if (s == null) {
                continue;
            }
            outTradeNo = keyMap.get(s);
            if (outTradeNo != null) {
                return dataMap.get(outTradeNo);
            }
        }

        return Tables.EMPTY;
    }

}
