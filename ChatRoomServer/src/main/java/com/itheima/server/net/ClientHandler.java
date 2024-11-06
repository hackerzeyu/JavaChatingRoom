package com.itheima.server.net;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.itheima.server.pojo.Message;
import com.itheima.server.common.Utility;
import com.itheima.server.sql.MysqlHandler;

import java.io.*;
import java.net.Socket;
import java.nio.Buffer;
import java.sql.SQLException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.itheima.server.net.ChatServer.clients;
import static com.itheima.server.net.ChatServer.onlines;

// 客户端处理类
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private OutputStream out;
    private InputStream in;
    // mysql操作对象
    private static MysqlHandler mysqlHandler=new MysqlHandler();
    // 互斥锁
    private Lock onlineLock=new ReentrantLock();

    // 构造函数
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.in=socket.getInputStream();
            this.out=socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loginHandle(Message obj) throws IOException {
        String userName=obj.getData1();
        String password=obj.getData2();
        Message msg;
        try {
            msg=mysqlHandler.loginHandle(userName,password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if(msg.getInfo().equals("login_success")){
            // 添加到在线用户队列里
            onlineLock.lock();
            onlines.add(new UserOnline(userName,clientSocket));
            onlineLock.unlock();
        }
        Utility.sendMsg(out,Utility.serializeJson(msg));
    }

    private void registerHandle(Message obj) throws IOException {
        String userName=obj.getData1();
        String password=obj.getData2();
        String name=obj.getData3();
        String json=null;
        try {
            json=mysqlHandler.registerHandle(userName,password,name);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Utility.sendMsg(out,json);
    }

    private void sendHandle(Message msg) throws IOException {
        String sender=msg.getData1();
        String receiver=msg.getData2();
        // 根据用户名找人,而不是根据昵称找人
        String receiverUserName=null;
        try {
            receiverUserName=mysqlHandler.getUserName(receiver);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if(receiverUserName==null)
            return;
        System.out.println("receiverUserName="+receiverUserName);
        int index=-1;
        for(int i=0;i<ChatServer.onlines.size();i++) {
            // 寻找接收用户
            if(onlines.get(i).getUsername().equals(receiverUserName)){
                index=i;
                break;
            }
        }
        if(index==-1){
            // 对方不在线
            Message sendMsg=new Message("send_reply",receiver,"","","offline");
            Utility.sendMsg(out,Utility.serializeJson(sendMsg));
            System.out.println("sendMsg="+Utility.serializeJson(sendMsg));
            return;
        }
        // 回给发送人
        Message sendMsg=new Message("send_reply",receiver,msg.getData3(),"","send_success");
        Utility.sendMsg(out,Utility.serializeJson(sendMsg));
        // 回给接收方
        Message sendMsg2=new Message("recv",sender,receiver,msg.getData3(),"");
        System.out.println("sendMsg="+Utility.serializeJson(sendMsg2));
        // 从在线队列中取出相应的socket
        Utility.sendMsg(onlines.get(index).getSocket().getOutputStream(),Utility.serializeJson(sendMsg2));
    }

    private void handleRecvMsg(String json) throws IOException {
        Message msg;
        try {
            msg= Utility.deserializeJson(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String cmd=msg.getCmd();
        switch (cmd) {
            case "login":
                loginHandle(msg);
                break;
            case "register":
                registerHandle(msg);
                break;
            case "send":
                sendHandle(msg);
                break;
            case "exit":
                exitHandle(msg);
            default:
                break;
        }
    }

    private void exitHandle(Message msg) {
        String userName=msg.getData1();
        onlineLock.lock();
        for(int i=0;i<ChatServer.onlines.size();i++) {
            if(onlines.get(i).getUsername().equals(userName)){
                onlines.remove(i);
            }
        }
        onlineLock.unlock();
        // 更新登录状态
        try {
            mysqlHandler.updateStatus(userName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (true) {
            String json= null;
            try {
                json = Utility.recvMsg(in);
                System.out.println("客户端发送信息:" + json);
                handleRecvMsg(json);
            } catch (IOException e) {
                try {
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                clients.remove(this);
            }
        }
    }
}