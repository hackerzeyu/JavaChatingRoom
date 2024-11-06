package com.itheima.client.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.itheima.client.common.Path;
import com.itheima.client.common.Utility;
import com.itheima.client.pojo.Message;
import com.sun.tools.javac.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ListWidget {
    private JFrame jf;
    private JList<String> friendsList;
    private DefaultListModel<String> friendListModel;
    public static ArrayList<MainWidget> chatFrames;
    private String userName;
    private String name;
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public void init(String friends){
        jf=new JFrame(name);
        try {
            jf.setIconImage(ImageIO.read(new File(Path.getPath("OIP.jpg"))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        jf.setSize(400, 500);
        jf.setResizable(false);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setLocationRelativeTo(null);

        // 好友列表数据
        friendListModel = new DefaultListModel<>();
        if(friends!=null) {
            String[] friendList = friends.split("\\|");
            for (String s : friendList) {
                friendListModel.addElement(s);
            }
        }
        friendsList = new JList<>(friendListModel);
        // 设置单项选择
        friendsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 滚动条
        JScrollPane scrollPane = new JScrollPane(friendsList);
        // 添加鼠标监听器来检测双击事件
        friendsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 检测是否是双击
                if (e.getClickCount() == 2) {
                    // 获取双击时选中的好友
                    String selectedFriend = friendsList.getSelectedValue();
                    if (selectedFriend != null) {
                        openChatWindow(selectedFriend);
                    }
                }
            }
        });
        // 主面板布局
        jf.add(scrollPane, BorderLayout.CENTER);
        chatFrames = new ArrayList<>();

        jf.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Message msg=new Message("exit",userName,"","","");
                String jsonStr=null;
                try {
                    jsonStr=Utility.serializeJson(msg);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                try {
                    Utility.sendMsg(out,jsonStr);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        jf.setVisible(true);
    }

    public ListWidget(Socket socket,String userName,String name,String friend_list) {
        this.userName=userName;
        this.name=name;
        init(friend_list);
        this.socket=socket;
        try {
            in=socket.getInputStream();
            out=socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 创建接收线程
        Thread recv=new Thread(new RecvTask());
        recv.start();
    }
    private class RecvTask implements Runnable{
        private void sendReplyHandle(Message msg){
            // 获取对方名字
            String chatName=msg.getData1();
            if(msg.getInfo().equals("offline")){
                for(int i=0;i<chatFrames.size();i++){
                    if(chatFrames.get(i).getTitle().equals(chatName)){
                        chatFrames.get(i).toFront();
                        String info=chatName+"不在线!";
                        JOptionPane.showMessageDialog(chatFrames.get(i),info,"提示",JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }else if(msg.getInfo().equals("send_success")){
                for(int i=0;i<chatFrames.size();i++){
                    if(chatFrames.get(i).getTitle().equals(chatName)){
                        // 设为前台显示
                        chatFrames.get(i).toFront();
                        // 获取当前日期和时间
                        LocalDateTime currentDateTime = LocalDateTime.now();
                        // 定义日期时间格式
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH:mm:ss");
                        // 格式化日期时间
                        String nowTime = currentDateTime.format(formatter);
                        StringBuilder builder=new StringBuilder();
                        String sendMsg=msg.getData2();
                        String line=builder.append(nowTime).append("_").append("你:").append(sendMsg).append("\n").toString();
                        final MainWidget widget=chatFrames.get(i);
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                widget.chatArea.append(line);
                                widget.inputField.setText("");
                            }
                        });
                    }
                }
            }
        }
        private void recvHandle(Message msg){
            String sender=msg.getData1();
            String receiver=msg.getData2();
            String msgInfo=msg.getData3();
            int index=-1;
            MainWidget widget = null;
            for(int i=0;i<chatFrames.size();i++){
                if(chatFrames.get(i).getTitle().equals(sender)) {
                    index = i;
                    widget=chatFrames.get(i);
                    break;
                }
            }
            if(index==-1){
                widget=new MainWidget(receiver,sender,socket);
                chatFrames.add(widget);
            }
            widget.toFront();
            // 获取当前日期和时间
            LocalDateTime currentDateTime = LocalDateTime.now();
            // 定义日期时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH:mm:ss");
            // 格式化日期时间
            String nowTime = currentDateTime.format(formatter);
            StringBuilder builder=new StringBuilder();
            String sendMsg=builder.append(nowTime).append("_").append(sender).append(":").append(msgInfo).append("\n").toString();
            final MainWidget finalWidget = widget;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if(finalWidget.chatArea!=null) {
                        finalWidget.chatArea.append(sendMsg);
                    }
                }
            });
        }
        @Override
        public void run() {
            while (true){
                try {
                    String recvJson=Utility.recvMsg(in);
                    System.out.println("recvMsg="+recvJson);
                    Message msg=Utility.deserializeJson(recvJson);
                    String cmd=msg.getCmd();
                    switch (cmd){
                        case "send_reply":
                            sendReplyHandle(msg);
                            break;
                        case "recv":
                            recvHandle(msg);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // 打开聊天窗口
    private void openChatWindow(String friendName) {
        // 如果窗口已打开，不再重复创建
        for (MainWidget widget : chatFrames) {
            if (widget.getTitle().equals(friendName)) {
                widget.setVisible(true);
                // 设置前台显示
                widget.toFront();
                return;
            }
        }
        // 创建新的聊天窗口
        MainWidget widget=new MainWidget(name,friendName,socket);
        chatFrames.add(widget);
    }
}
