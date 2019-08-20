package com.flag.monitoring_experiment.tcpUtils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.ObservableEmitter;

/**
 * 该Runnable 为wifi监听采集节点和wifi控制节点服务，不做任何zigbee节点的服务
 * Created by Bmind on 2018/6/15.
 */

public class IOBlockedRunnable implements Runnable {
    private Socket socket;                  //由客户端与服务器建立连接的socket
    public Socket getSocket(){
        return this.socket;
    }
    private BufferedReader br;              //读取对象
    public PrintWriter pw;                 //输出对象
    private String ipAddress;
    private String area;                    //区域11,21,12,22
    public String getArea(){
        return this.area;
    }
    private String type = "";               //类型 ,形如 "type#温湿度"
    private String number;                  //编号
    private int nowSeconds;                 //记录当前时间秒
    public int getNowTime(){
        return this.nowSeconds;
    }
    public void setNowTime(int time){
        this.nowSeconds = time;
    }
    public String TAG = "wifi线程任务：";
    private ObservableEmitter<MessageInfo> emitter;
    private boolean sendDataToMain = false; //是否将数据发送回主线程

    private String engType = "";     //字母类型，与前面的sensorType中文类型相对

    public String getEngType() {
        return engType;
    }

    public void setEngType(String engType) {
        this.engType = engType;
    }

    public String getType() {
        return type;
    }

    public String getNumber() {
        return number;
    }

    public IOBlockedRunnable(Socket socket, String area, ObservableEmitter<MessageInfo> emitter){
        if(socket.isConnected()){
            this.socket = socket;
            this.area = area;
            this.emitter = emitter;
            try {
                this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.pw = new PrintWriter(socket.getOutputStream(),true);
                this.ipAddress = socket.getInetAddress().toString().trim();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void closeSocket(){                    //断开与客户端的连接
        if (socket!=null){
            try {
                socket.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }
    public int getNowSeconds(){                 //获取时间秒
        SimpleDateFormat df = new SimpleDateFormat("mm:ss");
        String timestr = df.format(new Date().getTime());
        String[] times = timestr.split(":");
        int miao = Integer.parseInt(times[0].trim())*60 + Integer.parseInt(times[1].trim());
        return miao;
    }
    @Override
    public void run() {
        String msg = null;
        try {
            int len = 0;
            char[] buf = new char[17];
            boolean isSureType = false;
            String wifiIP = "";
            boolean isCollect = false;          //是否是采集节点
            StringBuilder sb = new StringBuilder();
            while((len = br.read(buf))!=-1){
                String line = new String(buf,0,len);
                sb.append(line);
                if (!br.ready()){
                    String result = sb.toString();
                    sb.setLength(0);
//                    Log.d(TAG, type+"wifi获取原始数据: "+result);
                    if (!result.startsWith("Hwd") || !result.endsWith("T"))continue;//不满足条件
                    setNowTime(getNowSeconds());
                    String newEngType = result.substring(3,5);//获取的数据类型
                    //是否已经知道了连接的类型，如果已经知道就不去执行确认类型的代码
                    if (!isSureType ){
                        wifiIP = "wifiip#"+ipAddress+"#";
                        this.engType = newEngType;
                        switch (engType){
                            case "af":type = "type#风扇";break;
                            case "he":type = "type#温湿度";break;
                            case "ie":type = "type#光照度";break;
                            /**cssf新增应用节点**/
                            case "el":type = "type#电磁锁";break;
                            case "al":type = "type#可调灯";break;
                            case "re":type = "type#继电器";break;
                            case "or":type = "type#全向红外";break;
                            case "sl":type = "type#声光报警";break;

                            case "hi":type = "type#人体红外";break;
                            case "if":type = "type#红外对射";break;
                            case "ga":type = "type#可燃气体";break;
                            case "fl":type = "type#火焰气体";break;
                        }
                        this.number = result.substring(5,7);         //获取唯一序号
                        String numberRe = "#number#" + number;
                        if (!type.equals("")){
                            isSureType = true;                  //确认类型完成，以后再来数据，不会进入检测类型
                            emitter.onNext(new MessageInfo("link",wifiIP + type + numberRe));
                        }
                    }
                    //假如有进行切换传感器类型的操作，那么传感器类型必然改变
                    if (!newEngType.equals(this.engType)){
                        this.engType = newEngType;
                        String orignalType = ipAddress+"#"+type.split("#")[1]+number;//旧的类型的键，MainUIActivity中nodeLayoutMap集合
                        switch (newEngType){
                            case "af":type = "type#风扇";break;
                            case "he":type = "type#温湿度";break;
                            case "ie":type = "type#光照度";break;
                            /**cssf新增应用节点**/
                            case "el":type = "type#电磁锁";break;
                            case "al":type = "type#可调灯";break;
                            case "re":type = "type#继电器";break;
                            case "or":type = "type#全向红外";break;
                            case "sl":type = "type#声光报警";break;
                            case "hi":type = "type#人体红外";break;
                            case "if":type = "type#红外对射";break;
                            case "ga":type = "type#可燃气体";break;
                            case "fl":type = "type#火焰气体";break;

                        }
                        String newType = ipAddress+"#"+type.split("#")[1]+number;//新的类型的键，MainUIActivity中nodeLayoutMap集合
                        emitter.onNext(new MessageInfo("typeChange",orignalType+"&"+newType+"&"+type.split("#")[1]));
                    }
                    //返回前台原始数据
                    emitter.onNext(new MessageInfo("data",result));

                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
        //通知主线程
        emitter.onNext(new MessageInfo("exit",this.area+"#"+ipAddress+"#"+type.split("#")[1] + "#" +number));
        Log.d(TAG, ipAddress + type.split("#")[1] +"断开服务器连接");
    }
    //提示本地线程可以发送数据回到主线程，或者关闭发送,通过emitter.onNext()把数据发送回去
//    public void notifyRunSendToMain(boolean gg){
//        sendDataToMain = gg;
//    }
    //告知主线程ipAndType
    public void informMainIpAndType(String ipAndType){
        emitter.onNext(new MessageInfo("inform",ipAndType));
    }
}
