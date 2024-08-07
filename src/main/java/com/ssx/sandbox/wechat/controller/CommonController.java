package com.ssx.sandbox.wechat.controller;

import com.ssx.sandbox.wechat.data.Storage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 通用接口
 */
@RestController
public class CommonController extends BaseController {

    @Resource
    private Storage storage;

    @GetMapping(path = "/data/save")
    public void dataSave() {
        storage.saveToFile();
    }

    @GetMapping(path = "/data/clear")
    public void dataClear() {
        storage.saveToFile();
    }
}