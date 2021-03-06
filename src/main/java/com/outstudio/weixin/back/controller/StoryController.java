package com.outstudio.weixin.back.controller;

import com.outstudio.weixin.back.exception.InvalidFileTypeException;
import com.outstudio.weixin.back.exception.SystemErrorException;
import com.outstudio.weixin.common.po.StoryEntity;
import com.outstudio.weixin.common.service.StoryService;
import com.outstudio.weixin.common.utils.FileUtil;
import com.outstudio.weixin.common.utils.MessageVoUtil;
import com.outstudio.weixin.common.vo.MessageVo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by lmy on 2017/9/9.
 */
@RestController
@RequestMapping("/back")
public class StoryController {

    private static final String REDIRECT_URL = "";

    @Resource
    private StoryService storyService;

    @PostMapping("/stories")
    public MessageVo uploadStory(@RequestParam("audio") MultipartFile audio,
                                 HttpServletRequest request,
                                 @ModelAttribute StoryEntity storyEntity) {

        String fileType = FileUtil.getType(audio);
        if (!"mp3".equalsIgnoreCase(fileType))
            throw new InvalidFileTypeException("上传的音频格式应为MP3");

        String audioSrc;
        try {
            if (storyEntity.getIs_free().equals(1))
                audioSrc = FileUtil.saveUploadFileAsUrlPath(request, audio, "free");
            else
                audioSrc = FileUtil.saveUploadFileAsUrlPath(request, audio, "charge");
        } catch (IOException e) {
            e.printStackTrace();
            throw new SystemErrorException();
        }

        storyEntity.setSrc(audioSrc);

        storyService.addStory(storyEntity);
        return MessageVoUtil.created(REDIRECT_URL);
    }

    @PutMapping("/stories/{id}")
    public MessageVo modifyStory(@PathVariable("id") Integer id,
                                 @ModelAttribute StoryEntity storyEntity) {
        storyEntity.setId(id);
        int changedNum = storyService.modifyStory(storyEntity);
        if (changedNum == 1) {
            return MessageVoUtil.created(REDIRECT_URL, storyService.getStoryById(id));
        } else {
            return MessageVoUtil.resourceNotFound(REDIRECT_URL);
        }
    }

    @DeleteMapping("/stories/{id}")
    public MessageVo deleteStory(@PathVariable Integer id) {
        int changedNum = storyService.deleteStoryById(id);
        if (changedNum == 1) {
            return MessageVoUtil.noContent(REDIRECT_URL);
        } else {
            return MessageVoUtil.resourceNotFound(REDIRECT_URL);
        }
    }

}
