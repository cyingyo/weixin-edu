package com.outstudio.weixin.page.service;

import com.alibaba.fastjson.JSON;
import com.outstudio.weixin.common.service.ChargeService;
import com.outstudio.weixin.common.utils.DateUtil;
import com.outstudio.weixin.common.utils.LoggerUtil;
import com.outstudio.weixin.common.utils.XmlUtil;
import com.outstudio.weixin.core.shiro.token.TokenManager;
import com.outstudio.weixin.wechat.config.WeixinProperties;
import com.outstudio.weixin.wechat.pay.impl.WXPayConfigImpl;
import com.outstudio.weixin.wechat.pay.sdk.WXPay;
import com.outstudio.weixin.wechat.pay.sdk.WXPayConstants;
import com.outstudio.weixin.wechat.pay.sdk.WXPayUtil;
import com.outstudio.weixin.wechat.utils.PayUtil;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by lmy on 2017/12/27.
 */
@Service
public class PayService {

    private static final String backOffUrl = "http://www.here52.cn/open/page/wxpayDone";
    private WXPay wxPay;

    @Resource
    private ChargeService chargeService;

    @Resource
    private CacheManager cacheManager;

    public PayService() {
        try {
            WXPayConfigImpl config = WXPayConfigImpl.getInstance();
            wxPay = new WXPay(config, backOffUrl);
        } catch (Exception e) {
            LoggerUtil.error(getClass(), e.getMessage());
            e.printStackTrace();

        }
    }

    public Map<String, String> doPay(String fee, String ip) {
        HashMap<String, String> data = new HashMap<>();
        data.put("body", "vip充值");
        data.put("out_trade_no", DateUtil.getFormatDate());
        data.put("device_info", "WEB");
        data.put("total_fee", fee);
        data.put("spbill_create_ip", ip);
        data.put("trade_type", "JSAPI");
        data.put("openid", TokenManager.getWeixinToken().getOpenid());

        LoggerUtil.fmtDebug(getClass(), "支付信息", data.toString());

        return pay(data);
    }

    public Map<String, String> pay(Map<String, String> payData) {
        Map<String, String> result;
        try {
            result = wxPay.unifiedOrder(payData);
            processPayResult(result);
        } catch (Exception e) {
            result = new HashMap<>();
            result.put("status", "400");
            LoggerUtil.error(getClass(), e.getMessage());
            LoggerUtil.error(getClass(), JSON.toJSONString(result));
        }
        return result;
    }

    private void processPayResult(Map<String, String> result) throws Exception {
        if ("SUCCESS".equalsIgnoreCase(result.get("return_code"))) {

            if ("SUCCESS".equalsIgnoreCase(result.get("result_code"))) {
                chargeService.preCharge(TokenManager.getWeixinToken().getOpenid());
            } else {
                throw new Exception("err_code：" + result.get("err_code") + "，err_code_des：" + result.get("err_code_des"));
            }

        } else {
            throw new Exception("return_msg：" + result.get("return_msg"));
        }
    }

    public Map<String, String> getH5PayParams(Map<String, String> payResult) {

        Map<String, String> h5Result = new HashMap<>();

        //生成H5调起微信支付API相关参数（前端页面js的配置参数）
        long timestamp = DateUtil.getTimestampSeconds();//当前时间的时间戳（秒）
        String packages = "prepay_id=" + payResult.get("prepay_id");//订单详情扩展字符串
        String nonceStr = WXPayUtil.generateUUID();

        h5Result.put("appId", WeixinProperties.appID);//公众号appid
        h5Result.put("timeStamp", String.valueOf(timestamp));
        h5Result.put("nonceStr", nonceStr); //随机数
        h5Result.put("package", packages);
        h5Result.put("signType", WXPayConstants.HMACSHA256);//签名方式

        try {
            h5Result.put("paySign", getSign(h5Result));
            h5Result.put("status", "200");
        } catch (Exception e) {
            LoggerUtil.fmtDebug(getClass(), "生成Sign出错，message：-> {%s}", e.getMessage());
            h5Result.put("status", "401");
        }

        LoggerUtil.fmtDebug(getClass(), JSON.toJSONString(h5Result));

        //将时间戳和packages存入缓存，便于回掉的签名验证
        Cache cache = getPayCache();
        cache.put(new Element(nonceStr + "timeStamp", String.valueOf(timestamp)));
        cache.put(new Element(nonceStr + "package", packages));

        return h5Result;
    }

    private String getSign(Map<String, String> data) throws Exception {
        return WXPayUtil.generateSignature(data, WeixinProperties.appKey, WXPayConstants.SignType.HMACSHA256);
    }

    public String callBack(Map<String, String> payResult) {

        LoggerUtil.fmtDebug(getClass(), "response message is : -> {%s}", JSON.toJSONString(payResult));
        String return_code = payResult.get("return_code");
        String return_msg = payResult.get("return_msg");

        String result = "";
        try {
            if ("SUCCESS".equalsIgnoreCase(return_code)) {
                String result_code = payResult.get("result_code");
                if ("SUCCESS".equalsIgnoreCase(result_code)) {
                    if (confirmSign(payResult) && confirmSuccess(payResult.get("transaction_id"))) {
                        return paySuccess(payResult);
                    }
                }
            }
            result = payFail();
        } catch (Exception e) {
            LoggerUtil.error(getClass(), "map to xml error", e);
        }
        return result;
    }

    /**
     * 验证签名，防止数据泄漏，造成资金损失。
     */
    private boolean confirmSign(Map<String, String> payResult) throws Exception {
        String nonceStr = payResult.get("nonce_str");
        String sign = payResult.get("sign");

        Map<String, String> map = new HashMap<>();

        map.put("appId", payResult.get("appid"));
        map.put("signType", payResult.get("sign_type"));
        map.put("nonceStr", nonceStr);

        Cache cache = getPayCache();
        map.put("timeStamp", cache.get(nonceStr + "timeStamp").toString());
        map.put("package", cache.get(nonceStr + "package").toString());

        return Objects.equals(sign, getSign(map));
    }

    /**
     * 请求微信服务器查看是否支付成功，防止用户直接请求此回掉接口造成损失
     */
    private boolean confirmSuccess(String transactionId) {
        Map<String, String> params = new HashMap<>();
        params.put("appid", WeixinProperties.appID);
        params.put("mch_id", WeixinProperties.mchID);
        params.put("transaction_id", transactionId);
        params.put("nonce_str", WXPayUtil.generateNonceStr());

        try {
            params.put("sign", getSign(params));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, String> result = PayUtil.orderQuery(XmlUtil.map2Xml(params));

        return processOrderQueryResult(result);
    }

    private boolean processOrderQueryResult(Map<String, String> map) {
        String returnCode = map.get("return_code");
        String resultCode = map.get("result_code");
        if ("SUCCESS".equals(returnCode) && "SUCCESS".equals(resultCode)) {
            String tradeState = map.get("trade_state");
            if ("SUCCESS".equals(tradeState)) {
                return true;
            }
        }
        return false;
    }

    private String paySuccess(Map<String, String> payResult) throws Exception {
        // 成功将成功信息写进数据库，返回SUCCESS
        chargeService.charge(payResult);

        Map<String, String> result = new HashMap<>();
        result.put("return_code", "SUCCESS");
        result.put("return_msg", "OK");
        return WXPayUtil.mapToXml(result);
    }

    private String payFail() throws Exception {
        Map<String, String> result = new HashMap<>();
        result.put("return_code", "FAIL");
        result.put("return-msg", "微信支付回调失败");
        return WXPayUtil.mapToXml(result);
    }

    private Cache getPayCache() {
        return cacheManager.getCache("payCache");
    }
}
