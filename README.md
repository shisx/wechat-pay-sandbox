# 微信支付沙箱

这是一个简单的微信支付沙箱服务，该服务能够接收部分微信支付相关接口，并能响应模拟数据、发送回调通知。

## 数据存储

* 请求、响应的数据存储在内存中，重启后失效；
* 通过数据存储，可以实现简单的数据关联、校验。

## 已支持的功能
* 统一下单
* 查询订单
* 支付结果通知
* 周期代扣
  * 支付中签约
  * 签约结果通知
  * 查询签约结果
  * 申请解约
  * 扣费前通知
  * 申请扣费
