package com.ssx.sandbox.wechat.data;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据存储
 *
 * @author Brook
 * @version 1.0.0
 */
@Component
public class Storage {

    private static final Logger log = LoggerFactory.getLogger(Storage.class);

    private final static File data_file = new File("./data/data_map.json");
    private final static File key_file = new File("./data/key_map.json");
    private long lastUpdateTime = 0L;

    @Resource
    private MemoryDB memoryDB;

    public void saveToFile() {
        try {
            if (!data_file.getParentFile().exists()) {
                data_file.getParentFile().mkdirs();
            }
            Files.write(data_file.toPath(), JSONObject.toJSONString(memoryDB.getDataMap(), JSONWriter.Feature.IgnoreNonFieldGetter).getBytes(StandardCharsets.UTF_8));

            if (!key_file.getParentFile().exists()) {
                key_file.getParentFile().mkdirs();
            }
            Files.write(key_file.toPath(), JSONObject.toJSONString(memoryDB.getKeyMap()).getBytes(StandardCharsets.UTF_8));

            lastUpdateTime = memoryDB.getLastTime();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadFromFile() {
        try {
            if (!data_file.exists()) {
                return;
            }

            Map<String, Tables> dataMap = JSONObject.parseObject(new String(Files.readAllBytes(data_file.toPath())),
                    TypeReference.mapType(HashMap.class, String.class, Tables.class));

            Map<String, String> keyMap = JSONObject.parseObject(new String(Files.readAllBytes(key_file.toPath())),
                    TypeReference.mapType(HashMap.class, String.class, String.class));

            memoryDB.putMap(dataMap, keyMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(initialDelay = 60000L, fixedDelay = 60000L)
    public void _timerSave() {
        if (memoryDB.getLastTime() <= lastUpdateTime) {
            return;
        }

        log.debug("saving data to file");
        saveToFile();
    }

}
