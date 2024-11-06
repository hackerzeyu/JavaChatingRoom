package com.itheima.client.pojo;

public class Message {
    private String cmd;
    private String data1;
    private String data2;
    private String data3;
    private String info;

    public Message(){
    }

    public Message(String cmd, String data1, String data2, String data3, String info) {
        this.cmd = cmd;
        this.data1 = data1;
        this.data2 = data2;
        this.data3 = data3;
        this.info = info;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public String getData2() {
        return data2;
    }

    public void setData2(String data2) {
        this.data2 = data2;
    }

    public String getData3() {
        return data3;
    }

    public void setData3(String data3) {
        this.data3 = data3;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
