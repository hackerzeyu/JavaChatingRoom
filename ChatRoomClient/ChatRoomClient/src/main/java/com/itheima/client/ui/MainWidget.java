package com.itheima.client.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.itheima.client.pojo.Message;
import com.itheima.client.common.Path;
import com.itheima.client.common.Utility;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MainWidget extends JFrame{
    // 历史消息存档
    private FileInputStream fis;
    private FileOutputStream fos;
    private String fileName;
    // 消息区
    protected JTextArea chatArea;
    // 输入区
    protected JTextField inputField;
    // 发送按钮
    private JButton sendButton;
    // 聊天的自身账号
    private String name;
    // 聊天的对方账号
    private String chatName;    // 聊天对象
    Socket socket;
    // 输入输出流
    private InputStream in;
    private OutputStream out;

    private static final int WIDTH=600;
    private static final int HEIGHT=400;

    public MainWidget(String name,String friendName,Socket socket){
        this.name=name;
        this.chatName=friendName;
        fileName=name+"&"+chatName+".txt";
        try {
            init();
            this.socket=socket;
            this.in=socket.getInputStream();
            this.out=socket.getOutputStream();
            File file=new File(fileName);
            // 文件存在，才能打开
            if(file.exists()) {
                fis = new FileInputStream(file);
                byte[] bytes = new byte[1024];
                int len = 0;
                while ((len = fis.read(bytes)) != -1) {
                    chatArea.append(new String(bytes, 0, len));
                }
            }
            if(fis!=null){
                fis.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void init() throws IOException {
        // 设置窗口标题
        setTitle(chatName);
        // 设置窗口图标
        setIconImage(ImageIO.read(new File(Path.getPath("OIP.jpg"))));
        setSize(WIDTH,HEIGHT);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        // 编辑区
        chatArea = new JTextArea(20,20);
        // 设置消息区不可编辑
        chatArea.setEditable(false);
        // 设置自动换行
        chatArea.setLineWrap(true);
        // 避免单词被拆分
        chatArea.setWrapStyleWord(true);
        // 为消息区设置滚动条
        JScrollPane scrollPane = new JScrollPane(chatArea);
        // 输入区
        inputField= new JTextField(20);
        // 发送按钮
        sendButton = new JButton("发送");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 发送信息
                try {
                    sendMessage();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        inputField.addActionListener((ActionEvent e)-> {
            try {
                sendMessage();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        // 设置布局
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Add components to frame
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                for(int i=0;i<ListWidget.chatFrames.size();i++){
                    if(ListWidget.chatFrames.get(i).getTitle().equals(chatName)){
                        System.out.println("remove successfully");
                        ListWidget.chatFrames.remove(i);
                    }
                }
                // 存档历史消息
                try {
                    fos=new FileOutputStream(fileName,true);
                    fos.write(chatArea.getText().getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }finally {
                    try {
                        fos.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                dispose();
            }
        });
    }

    private void sendMessage() throws IOException {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            String sendMsg=inputField.getText();
            Message msg=new Message("send",name,chatName,sendMsg,"");
            String jsonStr=Utility.serializeJson(msg);
            System.out.println("sendMsg="+jsonStr);
            Utility.sendMsg(out,jsonStr);
        }
    }
}
