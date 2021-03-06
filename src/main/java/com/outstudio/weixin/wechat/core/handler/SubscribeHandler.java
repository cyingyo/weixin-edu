package com.outstudio.weixin.wechat.core.handler;

import com.outstudio.weixin.common.po.UserEntity;
import com.outstudio.weixin.common.service.UserService;
import com.outstudio.weixin.common.utils.LoggerUtil;
import com.outstudio.weixin.wechat.utils.ContentUtil;
import com.outstudio.weixin.wechat.utils.MessageUtil;
import com.outstudio.weixin.wechat.utils.WechatUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Created by 96428 on 2017/9/7.
 * This in weixin-edu, com.outstudio.weixin.wechat.core.handler
 */
@Service("subscribeHandler")
public class SubscribeHandler implements Handler {

    @Resource
    private UserService userService;

    @Resource
    private ContentUtil contentUtil;

    /**
     * 当用户关注公众号时, 自动拉取用户信息, 保存在用户数据库中
     *
     * @param messageMap 获得到的信息
     * @return 问候消息
     */
    @Override
    public String handler(Map<String, String> messageMap) {
        String fromUser = messageMap.get("ToUserName");
        String userOpenid = messageMap.get("FromUserName");

        updateLocalUser(userOpenid);

        setPid(userOpenid, messageMap);

        Integer newsCount = WechatUtil.getMaterialCount("news_count");
        String offset = String.valueOf(newsCount - 1);
        return MessageUtil.createArticlesMessageXml(fromUser, userOpenid, contentUtil.news(offset,"1"));
//        return MessageUtil.createTextMessageXml(fromUser, userOpenid, contentUtil.onSubscribe(found.getNickname()));
    }

    /**
     * 如果数据库中存在用户,就更新用户信息;如果不存在,将用户信息保存在数据库中
     * @param userOpenid
     */
    private void updateLocalUser(String userOpenid) {

        UserEntity found = userService.getUserByOpenId(userOpenid);
        UserEntity user = WechatUtil.getUserInfoOnSubscribe(userOpenid);
        if (found == null) {
            userService.saveUser(user);
        } else {
            userService.updateUser(user);
        }
    }

    /**
     * 根据传来的EventKey设置当前用户的pid
     * @param userOpenid
     * @param messageMap
     */
    private void setPid(String userOpenid, Map<String, String> messageMap) {
        UserEntity now = userService.getUserByOpenId(userOpenid);
        String EventKey = messageMap.get("EventKey");
        LoggerUtil.fmtDebug(getClass(), "message：{%s}", messageMap);
        if (EventKey != null && !EventKey.isEmpty()) {
            int pid = Integer.parseInt(EventKey.substring(8));

            if (now.getId() == pid) {
                //do nothing
            }

            if (now.getPid() == 0) {
                userService.setPid(pid, userOpenid);
            }
        }
    }
}
