package com.example.demo.model;

import com.example.demo.util.ExceptionHelper;
import lombok.Getter;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author itw_wangjb03
 * @date 2018/6/12
 * sprint by itw_wangjb03：用于
 */
@Getter
public class ResponseMessage implements BaseBo {
    /**
     * 时间戳
     */
    private Long timestamp;
    /**
     * 成功状态
     */
    private Boolean success;
    /**
     * 状态码
     */
    private Integer code;
    /**
     * 消息内容
     */
    private String message;
    /**
     * 数据存放字段
     */
    private Object data;

    //成功构造
    public static ResponseMessage ok() {
        return ok(null);
    }


    public static ResponseMessage ok(Object data) {
        ResponseMessage msg = new ResponseMessage();
        msg.timeStamp().code(200).data(data).success(Boolean.TRUE);
        return msg;
    }


    //失败构造
    public static ResponseMessage error(Exception ex) {
        return error(500, ExceptionHelper.getBootMessage(ex));
    }

    public static ResponseMessage error(String message) {
        return error(500, message);
    }


    public static ResponseMessage error(int code, String message) {
        ResponseMessage msg = new ResponseMessage();
        msg.code(code).message(message).timeStamp().success(Boolean.FALSE);
        return msg;
    }


    //设置报文头信息
    private ResponseMessage timeStamp() {
        this.timeStamp(System.currentTimeMillis());
        return this;
    }

    private ResponseMessage timeStamp(Long timeStamp) {
        this.timestamp = timeStamp;
        return this;
    }

    public ResponseMessage success(Boolean success){
        this.success=success;
        return this;
    }

    public ResponseMessage code(int code) {
        this.code = code;
        return this;
    }

    public ResponseMessage message(String message) {
        this.message = message;
        return this;
    }


    //设置数据信息
    public <T> ResponseMessage data(T data) {
        //处理分页信息
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this , ToStringStyle.SHORT_PREFIX_STYLE );
    }


}
