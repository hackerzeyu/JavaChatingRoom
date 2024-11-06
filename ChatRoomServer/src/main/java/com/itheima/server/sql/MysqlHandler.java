package com.itheima.server.sql;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.itheima.server.net.ChatServer;
import com.itheima.server.net.UserOnline;
import com.itheima.server.pojo.Message;
import com.itheima.server.common.Utility;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MysqlHandler {
    // 数据库连接池获取
    private DataSource dataSource;

    public MysqlHandler(){
        try {
            initSql();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void initSql() throws Exception {
        System.out.println(System.getProperty("user.dir"));
        Properties prop=new Properties();
        prop.load(new FileInputStream("src\\main\\resources\\db.properties"));
        dataSource= DruidDataSourceFactory.createDataSource(prop);
    }

    // 获取数据库连接
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // 登录处理
    public synchronized Message loginHandle(String userName,String password) throws SQLException {
        Connection conn=getConnection();
        PreparedStatement ps=conn.prepareStatement("select * from tb_user where username=? and password=?");
        ps.setString(1,userName);
        ps.setString(2,password);
        ResultSet rs=ps.executeQuery();
        Message msg=null;
        if(rs.next()){
            // 获取状态
            int status=rs.getInt("status");
            if(status==1){
                // 用户已经登录
                msg=new Message("login_reply","","","","user_login_already");
                rs.close();
                ps.close();
                conn.close();
                return msg;
            }
            ps.close();
            // 查询好友列表信息
            ps=conn.prepareStatement("select name,friend_list from tb_user_info where username=?");
            ps.setString(1,userName);
            ResultSet rs2=ps.executeQuery();
            if(rs2.next()) {
                String name = rs2.getString("name");
                String friendList = rs2.getString("friend_list");
                if (friendList == null)
                    msg = new Message("login_reply", name, "", "", "login_success");
                else
                    msg = new Message("login_reply", name, friendList, "", "login_success");
            }else {
                throw new SQLException();
            }
            ps.close();
            ps=conn.prepareStatement("update tb_user set status=1 where username=?");
            ps.setString(1,userName);
            ps.executeUpdate();
            rs2.close();
        }else {
            // 登陆失败
            msg=new Message("login_reply","","","","login_failed");
        }
        rs.close();
        ps.close();
        conn.close();
        // 序列化
        return msg;
    }

    // 注册处理
    public synchronized String registerHandle(String userName,String password,String name) throws SQLException, JsonProcessingException {
        Connection conn=getConnection();
        PreparedStatement ps=conn.prepareStatement("select * from tb_user where username=?");
        ps.setString(1,userName);
        Message msg=null;
        ResultSet rs=ps.executeQuery();
        // 账号已经存在
        if(rs.next()){
            msg=new Message("register_reply","","","","username_exist");
            rs.close();;
            ps.close();
            conn.close();
            return Utility.serializeJson(msg);
        }
        rs.close();
        ps.close();
        ps=conn.prepareStatement("insert into tb_user(username,password) values (?,?)");
        ps.setString(1,userName);
        ps.setString(2,password);
        // 设置事务
        conn.setAutoCommit(false);
        int count=ps.executeUpdate();
        if(count<=0){
            msg=new Message("register_reply","","","","register_failed");
            conn.rollback();
            ps.close();
            conn.close();
            return Utility.serializeJson(msg);
        }
        ps.close();
        ps=conn.prepareStatement("insert into tb_user_info(username,name) values (?,?)");
        ps.setString(1,userName);
        ps.setString(2,name);
        count=ps.executeUpdate();
        if(count<=0){
            msg=new Message("register_reply","","","","register_failed");
            conn.rollback();
            ps.close();
            conn.close();
            return Utility.serializeJson(msg);
        }
        msg=new Message("register_reply","","","","register_success");
        conn.commit();
        ps.close();
        conn.close();
        return Utility.serializeJson(msg);
    }

   public synchronized String getUserName(String name) throws SQLException {
        Connection connection=dataSource.getConnection();
        // 查询其账号
        PreparedStatement ps=connection.prepareStatement("select username from tb_user_info where name=?");
        ps.setString(1,name);
        ResultSet rs=ps.executeQuery();
        String result=null;
        if(rs.next()){
            result=rs.getString("username");
        }
        rs.close();
        ps.close();
        connection.close();
        return result;
    }

    public synchronized boolean updateStatus(String userName) throws SQLException {
        Connection connection=dataSource.getConnection();
        // 查询账号
        PreparedStatement ps=connection.prepareStatement("update tb_user set status=0 where username=?");
        ps.setString(1,userName);
        int count=ps.executeUpdate();
        ps.close();
        connection.close();
        return count>0;
    }

}
