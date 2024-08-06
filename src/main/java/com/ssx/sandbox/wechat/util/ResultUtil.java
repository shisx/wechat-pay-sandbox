// package com.ssx.iiv.util;
//
// import com.github.binarywang.wxpay.bean.result.BaseWxPayResult;
// import org.apache.commons.lang3.RandomStringUtils;
//
// import java.io.File;
// import java.nio.file.Files;
//
// public class ResultUtil {
//
//     private static final String API_DIR = System.getProperty("result_dir");
//     private static final String SUCCESS = "SUCCESS";
//     private static final String FAIL = "FAIL";
//
//     public static String loadResult(String fileName) {
//         try {
//             File file = new File(API_DIR, fileName + ".xml");
//             return new String(Files.readAllBytes(file.toPath()));
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//         return null;
//     }
//
//     public static <T extends BaseWxPayResult> T loadResult(String fileName, Class<T> clazz) {
//         String xml = loadResult(fileName);
//         return T.fromXML(xml, clazz);
//     }
//
//     public static void success(BaseWxPayResult result) {
//         result.setReturnCode(SUCCESS);
//         result.setResultCode(SUCCESS);
//
//         result.setAppid(PROPERTIES.getAppId());
//         result.setMchId(PROPERTIES.getMchId());
//
//         result.setNonceStr(RandomStringUtils.random(32, true, true));
//     }
//
// }
