package com.itheima.server.net;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    // 定义端口为8000
    private static final int PORT = 9000;
    public static List<ClientHandler> clients = new ArrayList<>();
    public static List<UserOnline> onlines=new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chatserver started on port " + PORT+"...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("new client connected: " + clientSocket.getInetAddress().getHostAddress());
                // 创建一个新的客户端处理线程
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                // 启动该客户端的线程
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
