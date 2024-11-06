package com.itheima.server.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itheima.server.pojo.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// 工具类
public class Utility {
    // 发送数据
    public static void sendMsg(OutputStream out, String msg) throws IOException {
        out.write(msg.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
    // 接收数据
    public static String recvMsg(InputStream in) throws IOException {
        byte[] bytes = new byte[1024];
        int len = in.read(bytes);
        return new String(bytes,0,len, StandardCharsets.UTF_8);
    }
    // 序列化json
    public static String serializeJson(Message msg) throws JsonProcessingException {
        ObjectMapper mapper=new ObjectMapper();
        return mapper.writeValueAsString(msg);
    }
    // 反序列化json
    public static Message deserializeJson(String json) throws JsonProcessingException {
        ObjectMapper mapper=new ObjectMapper();
        return mapper.readValue(json,Message.class);
    }
}
