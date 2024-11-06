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

public class RegisterWidget {
    // 窗口
    JFrame jf;
    // 窗口宽高
    private final int WIDTH=500;
    private final int HEIGHT=400;
    // 通信信息
    private final String IP="localhost";
    private final int PORT=9000;
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public RegisterWidget(Socket socket){
        this.socket=socket;
        try {
            this.in=socket.getInputStream();
            this.out=socket.getOutputStream();
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void init() throws IOException {
        jf=new JFrame("聊天室");
        // 设置窗口基本属性
        int screenWidth=Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight=Toolkit.getDefaultToolkit().getScreenSize().height;
        jf.setBounds((screenWidth-WIDTH)/2,(screenHeight-HEIGHT)/2,WIDTH,HEIGHT);
        jf.setResizable(false);
        jf.setIconImage(ImageIO.read(new File(Path.getPath("OIP.jpg"))));

        // 背景图片
        BackGroundPanel backGroundPanel = new BackGroundPanel(ImageIO.read(new File(Path.getPath("bg2.jpg"))));
        backGroundPanel.setBounds(0,0,WIDTH,HEIGHT);

        // 组装视图
        Box vBox=Box.createVerticalBox();
        // 昵称
        Box nameBox=Box.createHorizontalBox();
        JLabel nameLabel=new JLabel("昵        称:");
        JTextField nameField=new JTextField(15);
        nameBox.add(nameLabel);
        nameBox.add(Box.createHorizontalStrut(10));
        nameBox.add(nameField);
        // 用户名
        Box uBox=Box.createHorizontalBox();
        JLabel uLabel=new JLabel("用  户  名:");
        JTextField uField=new JTextField(15);
        uBox.add(uLabel);
        uBox.add(Box.createHorizontalStrut(10));
        uBox.add(uField);
        // 密码
        Box pBox=Box.createHorizontalBox();
        JLabel pLabel=new JLabel("密        码:");
        JPasswordField pField=new JPasswordField(15);
        pBox.add(pLabel);
        pBox.add(Box.createHorizontalStrut(10));
        pBox.add(pField);
        // 确认密码
        Box rpBox=Box.createHorizontalBox();
        JLabel rpLabel=new JLabel("确认密码:");
        JPasswordField rpField=new JPasswordField(15);
        rpBox.add(rpLabel);
        rpBox.add(Box.createHorizontalStrut(10));
        rpBox.add(rpField);
        // 注册按钮
        Box btnBox=Box.createHorizontalBox();
        JButton registerBtn=new JButton("注册");
        JButton backBtn=new JButton("返回");
        btnBox.add(registerBtn);
        btnBox.add(Box.createHorizontalStrut(110));
        btnBox.add(backBtn);

        // 添加事件
        registerBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name=nameField.getText().trim();
                if(name.isEmpty()){
                    JOptionPane.showMessageDialog(jf,"昵称为空!","警告",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String userName=uField.getText().trim();
                if(userName.isEmpty()){
                    JOptionPane.showMessageDialog(jf,"用户名为空!","警告",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String password=pField.getText().trim();
                if(password.isEmpty()){
                    JOptionPane.showMessageDialog(jf,"密码为空!","警告",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String rpPassword=rpField.getText().trim();
                if(rpPassword.isEmpty()){
                    JOptionPane.showMessageDialog(jf,"确认密码为空!","警告",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if(!rpPassword.equals(password)){
                    JOptionPane.showMessageDialog(jf,"两次密码输入不一致!","警告",JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Message msg=new Message("register",userName,password,name,"");
                String jsonStr;
                try {
                    jsonStr=Utility.serializeJson(msg);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                String recvStr=null;
                try {
                    System.out.println(jsonStr);
                    Utility.sendMsg(out, jsonStr);
                    recvStr = Utility.recvMsg(in);
                }catch (IOException ex) {
                    try {
                        in.close();
                        out.close();
                        socket.close();
                    } catch (IOException exc) {
                        JOptionPane.showMessageDialog(jf,"服务器异常!","错误",JOptionPane.WARNING_MESSAGE);
                        throw new RuntimeException(exc);
                    }
                }
                Message recvMsg;
                try {
                    recvMsg=Utility.deserializeJson(recvStr);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                String info=recvMsg.getInfo();
                if(info.equals("register_success")){
                    JOptionPane.showMessageDialog(jf,"注册成功!","提示",JOptionPane.INFORMATION_MESSAGE);
                }else if(info.equals("username_exist")){
                    JOptionPane.showMessageDialog(jf,"用户名已经存在!","提示",JOptionPane.INFORMATION_MESSAGE);
                }else {
                    JOptionPane.showMessageDialog(jf,"注册失败!","错误",JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        backBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jf.dispose();
                new LoginWidget(socket);
            }
        });

        vBox.add(Box.createVerticalStrut(100));
        vBox.add(nameBox);
        vBox.add(Box.createVerticalStrut(20));
        vBox.add(uBox);
        vBox.add(Box.createVerticalStrut(20));
        vBox.add(pBox);
        vBox.add(Box.createVerticalStrut(20));
        vBox.add(rpBox);
        vBox.add(Box.createVerticalStrut(20));
        vBox.add(btnBox);

        backGroundPanel.add(vBox);
        jf.add(backGroundPanel);
        jf.setVisible(true);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
