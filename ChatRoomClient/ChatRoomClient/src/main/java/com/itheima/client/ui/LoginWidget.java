package com.itheima.client.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.itheima.client.component.BackGroundPanel;
import com.itheima.client.pojo.Message;
import com.itheima.client.common.Path;
import com.itheima.client.common.Utility;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class LoginWidget {
    // 窗口
    JFrame jf;
    // 窗口宽高
    private final int WIDTH=500;
    private final int HEIGHT=300;
    // 通信信息
    private final String IP="localhost";
    private final int PORT=9000;
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    public LoginWidget(){
        try {
            init();
            initConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public LoginWidget(Socket socket){
        this.socket=socket;
        try {
            this.in=socket.getInputStream();
            this.out=socket.getOutputStream();
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // 组装视图
    public void init() throws IOException{
        jf=new JFrame("聊天室");
        // 设置窗口相关的属性
        // 设置窗口居中
        int screenWidth= Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight= Toolkit.getDefaultToolkit().getScreenSize().height;
        jf.setBounds((screenWidth-WIDTH)/2,(screenHeight-HEIGHT)/2,WIDTH,HEIGHT);
        // 设置不可改变大小
        jf.setResizable(false);
        // 设置窗口图标
        jf.setIconImage(ImageIO.read(new File(Path.getPath("OIP.jpg"))));
        // 设置窗口的内容
        BackGroundPanel bgPanel=new BackGroundPanel(ImageIO.read(new File(Path.getPath("bg.jpg"))));
        // 组装登录相关的元素
        Box vBox=Box.createVerticalBox();
        // 组装用户名
        Box uBox=Box.createHorizontalBox();
        JLabel uLabel=new JLabel("用户名:");
        JTextField uField=new JTextField(15);
        uBox.add(uLabel);
        uBox.add(Box.createHorizontalStrut(15));
        uBox.add(uField);
        // 组装密码
        Box pBox=Box.createHorizontalBox();
        JLabel pLabel=new JLabel("密    码:");
        JPasswordField pField=new JPasswordField(15);
        pBox.add(pLabel);
        pBox.add(Box.createHorizontalStrut(15));
        pBox.add(pField);
        // 组装按钮
        Box btnBox=Box.createHorizontalBox();
        JButton loginBtn=new JButton("登录");
        JButton registerBtn=new JButton("注册");

        // 登录按钮点击
        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 没建立上连接,没有必要向下执行了
                if(socket==null){
                    JOptionPane.showMessageDialog(jf,"未于服务器建立连接!","错误",JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // 获取用户输入的数据
                String userName=uField.getText().trim();
                String password=pField.getText().trim();
                // 组装登录数据
                if(userName.isEmpty()){
                    JOptionPane.showMessageDialog(jf,"用户名不能为空!","警告",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if(password.isEmpty()){
                    JOptionPane.showMessageDialog(jf,"密码不能为空!","警告",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Message msg=new Message("login",userName,password,"","");
                // 创建一个ObjectMapper对象,进行序列化
                String jsonStr;
                try {
                    jsonStr=Utility.serializeJson(msg);
                } catch (JsonProcessingException ex) {
                    JOptionPane.showMessageDialog(jf,"软件内部故障!","错误",JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String recvMsg=null;
                System.out.println("sendMsg="+jsonStr);
                // 发送数据
                try {
                    Utility.sendMsg(out,jsonStr);
                    // 等待接收
                    recvMsg=Utility.recvMsg(in);
                } catch (IOException ex) {
                    try {
                        JOptionPane.showMessageDialog(jf,"服务器异常!","错误",JOptionPane.ERROR_MESSAGE);
                        in.close();
                        out.close();
                        socket.close();
                    } catch (IOException exc) {
                        throw new RuntimeException(exc);
                    }
                }
                System.out.println("recvMsg="+recvMsg);
                // 反序列化
                Message msgRecv;
                try {
                    msgRecv=Utility.deserializeJson(recvMsg);
                } catch (JsonProcessingException ex) {
                    JOptionPane.showMessageDialog(jf,"软件内部故障!","错误",JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String info=msgRecv.getInfo();
                if(info.equals("login_success")){
                    JOptionPane.showMessageDialog(jf,"登陆成功!","提示",JOptionPane.INFORMATION_MESSAGE);
                    jf.dispose();
                    String name=msgRecv.getData1();
                    String friend_list=msgRecv.getData2();
                    new ListWidget(socket,userName,name,friend_list);
                }else if(info.equals("login_failed")){
                    JOptionPane.showMessageDialog(jf,"用户名或密码错误!","提示",JOptionPane.INFORMATION_MESSAGE);
                }else if(info.equals("user_login_already")){
                    JOptionPane.showMessageDialog(jf,"该用户已经登录!","提示",JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // 注册按钮点击
        registerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (socket == null) {
                    JOptionPane.showMessageDialog(jf, "未与服务器建立连接,不予注册!", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                jf.dispose();
                new RegisterWidget(socket);
            }
        });

        btnBox.add(loginBtn);
        btnBox.add(Box.createHorizontalStrut(100));
        btnBox.add(registerBtn);
        // 垂直布局
        vBox.add(Box.createVerticalStrut(50));
        vBox.add(uBox);
        vBox.add(Box.createVerticalStrut(20));
        vBox.add(pBox);
        vBox.add(Box.createVerticalStrut(50));
        vBox.add(btnBox);

        bgPanel.add(vBox);
        jf.add(bgPanel);
        jf.setVisible(true);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
    // 连接服务器
    public void initConnection(){
        try {
            socket=new Socket(IP,PORT);
            in=socket.getInputStream();
            out=socket.getOutputStream();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(jf,"连接服务器失败!","错误",JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }
    }
    // 客户端程序入口
    public static void main(String[] args) {
        LoginWidget lw=new LoginWidget();
    }
}
