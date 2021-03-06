package com.outstudio.weixin.wechat.controller;

import com.alibaba.fastjson.JSONObject;
import com.outstudio.weixin.common.po.UserEntity;
import com.outstudio.weixin.common.utils.LoggerUtil;
import com.outstudio.weixin.common.utils.OAuthUtil;
import com.outstudio.weixin.wechat.core.MyWechat;
import com.outstudio.weixin.wechat.dto.OAuthAccessToken;
import com.outstudio.weixin.wechat.dto.SignaturePo;
import com.outstudio.weixin.wechat.utils.MessageUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;

/**
 * Created by 96428 on 2017/7/13.
 */
@Controller
public class WeixinController {

    @Resource
    private MyWechat myWechat;

    @GetMapping(value = "/weixin")
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

        LoggerUtil.fmtDebug(getClass(), "验证完毕, 状态 -> {%s}", flag);

        return flag ? echostr : "";
    }

    @PostMapping(value = "/weixin")
    public void weixin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter pw = response.getWriter();

        myWechat.setRequest(request);
        String result = myWechat.execute();

        pw.write(result);
        pw.flush();
        pw.close();
    }

    @GetMapping("/oauth")
    public ModelAndView oauth(@RequestParam("code") String code,
                              @RequestParam("state") String state,//state包含授权成功后跳转到的页面
                              HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        OAuthAccessToken authAccessToken = OAuthUtil.getOAuthAccessToken(code);
        UserEntity userEntity = OAuthUtil.getUserInfo(authAccessToken);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("userInfo", userEntity);
        modelAndView.setViewName(state);
        return modelAndView;
    }
}
