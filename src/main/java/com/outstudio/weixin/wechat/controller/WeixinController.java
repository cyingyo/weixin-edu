package com.outstudio.weixin.wechat.controller;

import com.outstudio.weixin.common.utils.LoggerUtil;
import com.outstudio.weixin.wechat.core.MyWechat;
import com.outstudio.weixin.wechat.core.MyWechatFactory;
import com.outstudio.weixin.wechat.dto.SignaturePo;
import com.outstudio.weixin.wechat.utils.MessageUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by 96428 on 2017/7/13.
 */
@Controller
public class WeixinController {

    private Logger logger = Logger.getLogger(WeixinController.class);

    @RequestMapping(value = "/weixin.do", method = RequestMethod.GET)
    @ResponseBody
    public String weixin(
            @RequestParam String signature, @RequestParam String timestamp,
            @RequestParam String nonce, @RequestParam String echostr) {

        LoggerUtil.fmtDebug(getClass(),
                "开始验证, 输入的值为 : \nsignature->{%s},\n timestamp->{%s},\n nonce->{%s},\n echostr->{%s}",
                signature, timestamp, nonce, echostr);

        SignaturePo po = new SignaturePo();
        po.setSignature(signature);
        po.setTimestamp(timestamp);
        po.setNonce(nonce);
        po.setEchostr(echostr);
        boolean flag = MessageUtil.checkSignature(po);

        LoggerUtil.fmtDebug(getClass(), "验证完毕, 状态->{%s}", flag);

        return flag ? echostr : "";
    }

    @RequestMapping(value = "/weixin.do", method = RequestMethod.POST)
    public void weixin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter pw = response.getWriter();

        MyWechat wechat = MyWechatFactory.getMyWechat(request);
        String result = wechat.execute();

        pw.write(result);
        pw.flush();
        pw.close();
    }
}
