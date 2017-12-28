package com.outstudio.weixin.wechat.utils;

import com.alibaba.fastjson.JSONObject;
import com.outstudio.weixin.common.utils.NetWorkUtil;
import com.outstudio.weixin.wechat.config.WeixinProperties;
import org.dom4j.DocumentException;

import java.util.Map;

/**
 * Created by lmy on 2017/12/27.
 */
public class PayUtil {
    /**
     * 查询订单接口
     */
    public static Map<String, String> orderQuery(String params) {
        String result = NetWorkUtil.doPostUriReturnPlainText(WeixinProperties.ORDER_QUERY, params);
        Map<String, String> xml = null;
        try {
            xml = MessageUtil.xml2Map(result);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return xml;
    }

}
