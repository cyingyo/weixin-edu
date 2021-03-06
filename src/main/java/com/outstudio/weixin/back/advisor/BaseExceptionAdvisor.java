package com.outstudio.weixin.back.advisor;

import com.outstudio.weixin.back.exception.*;
import com.outstudio.weixin.common.consts.ResponseStatus;
import com.outstudio.weixin.common.vo.MessageVo;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Created by 96428 on 2017/8/7.
 * This in hospital, io.github.cyingyo.hospital.common.advisor
 */
@RestControllerAdvice
public class BaseExceptionAdvisor {

    @ExceptionHandler(UnvalidException.class)
    public MessageVo handleUnvalid(UnvalidException e) {
        MessageVo messageVo = new MessageVo();

        messageVo.setStatus(ResponseStatus.UNVALID_PARAM).setMessage(e.getMessage());
        if (e.getErrors() != null) messageVo.setData(e.getErrors().getAllErrors());

        return messageVo;
    }

    @ExceptionHandler(SystemErrorException.class)
    public MessageVo handleError(SystemErrorException e) {
        MessageVo messageVo = new MessageVo();

        messageVo.setStatus(ResponseStatus.SYSTEM_ERROE).setMessage(e.getMessage());

        return messageVo;
    }

    /**
     *
     * @param e
     * @return
     */
    @ExceptionHandler(NoresourceException.class)
    public MessageVo handleResource(NoresourceException e) {
        MessageVo messageVo = new MessageVo();

        messageVo.setStatus(ResponseStatus.RESOURCE_NOT_FOUND).setMessage(e.getMessage());

        return messageVo;
    }

    /**
     * 错误码 40004
     * @param e 没有权限
     * @return json化 messageVo
     */
    @ExceptionHandler(NoSuchPermissionException.class)
    public MessageVo handleNoPermis(NoSuchPermissionException e) {
        MessageVo messageVo = new MessageVo();

        messageVo.setStatus(ResponseStatus.NO_PERMISSION).setMessage(e.getMessage());

        return messageVo;
    }

    /**
     * 错误码 40005
     * @param e 用户不存在
     * @return json化 messageVo
     */
    @ExceptionHandler(NoSuchUserException.class)
    public MessageVo handleNoUser(NoSuchUserException e) {
        MessageVo messageVo = new MessageVo();

        messageVo.setStatus(ResponseStatus.NO_SUCH_USER).setMessage(e.getMessage());

        return messageVo;
    }

    @ExceptionHandler(WrongPasswordException.class)
    public MessageVo handleWrongToken(WrongPasswordException e) {
        MessageVo messageVo = new MessageVo();

        messageVo.setStatus(ResponseStatus.WRONG_TOKEN).setMessage(e.getMessage());

        return messageVo;
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public MessageVo handleInvalidFileType(InvalidFileTypeException e) {
        MessageVo messageVo = new MessageVo();

        messageVo.setStatus(ResponseStatus.WRONG_FILE_TYPE).setMessage(e.getMessage());

        return messageVo;
    }

    @ExceptionHandler(InvalidRequestException.class)
    public MessageVo handleInvalidRequest(InvalidRequestException e) {
        MessageVo messageVo = new MessageVo();

        messageVo.setMessage(e.getMessage()).setStatus(ResponseStatus.LOGICAL_ERROR);

        return messageVo;
    }

}
