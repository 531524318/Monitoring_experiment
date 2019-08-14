package com.flag.monitoring_experiment.tcpUtils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.reactivex.Emitter;
import io.reactivex.ObservableEmitter;

/**
 * 继承的IOBlockedRunnable 为zigbee节点服务，包括采集节点和控制节点
 * Created by Bmind on 2018/6/15.
 */

public class IOBlockedZigbeeRunnable extends IOBlockedRunnable {
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
    public static String TAG = "任务：";
    private String number;                  //编号
    private ObservableEmitter<MessageInfo> emitter;
//    private boolean sendDataToMainZigbee = false; //是否将数据发送回主线程

    private ArrayList<ZigbeeDevice> devicelist = new ArrayList<>();
    public ArrayList<ZigbeeDevice> getDeviceList(){
        return devicelist;
    }
    private int nowSeconds;                 //记录当前时间秒
    public int getNowTime(){
        return this.nowSeconds;
    }


    public IOBlockedZigbeeRunnable(Socket socket, String area, ObservableEmitter<MessageInfo> emitter){
        super( socket, area, emitter);
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
                e.printStackTrace();
            }
        }
    }
    @Override
    public void run() {
        try {
            int len = 0;
            char[] buf = new char[17];

            boolean isCollect = false;          //是否是采集类传感器
            StringBuilder sb = new StringBuilder();

            SimpleDateFormat df = new SimpleDateFormat("mm:ss");
            nowSeconds = getNowSeconds();
            while((len = br.read(buf))!=-1){
                String line = new String(buf,0,len);
                sb.append(line);
                if (!br.ready()){
                    String result = sb.toString();
                    sb.setLength(0);
//                    Log.d(TAG,"zigbee获取原始数据: " +result);
                    String[] datas = result.split("T");
                    for (String dataone : datas){
                        result = dataone + "T";
                        nowSeconds = getNowSeconds();
                        if (!result.startsWith("Hz") || !result.endsWith("T"))continue;
                        //获取时间：全部化成秒
                        String timestr = df.format(new Date().getTime());
                        String[] times = timestr.split(":");
                        int miao = Integer.parseInt(times[0].trim())*60 + Integer.parseInt(times[1].trim());
                        String number = result.substring(5,7);
                        ZigbeeDevice temp = findReadyExit(number,miao,emitter);
                        String newType = result.substring(3,5);
                        String type = "";
                        if (temp == null){
                            if (result.startsWith("Hz")&&result.endsWith("T")){
                                ZigbeeDevice zd = null;
                                switch (newType){
                                    case "wp":type = "type#水泵";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "af":type = "type#风扇";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "rs":type = "type#卷帘";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "pl":type = "type#植物生长灯";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "cr":type = "type#加热器";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "hr":type = "type#加湿器";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "he":type = "type#温湿度";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    case "ie":type = "type#光照度";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    case "on":type = "type#氧气";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    case "bp":type = "type#大气压";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    case "cd":type = "type#二氧化碳";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    case "du":type = "type#粉尘";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    /**cssf新增应用**/
                                    case "el":type = "type#电磁锁";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "al":type = "type#可调灯";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "re":type = "type#继电器";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "or":type = "type#全向红外";zd = new ZigbeeDevice(miao,number,type);break;
                                    case "sl":type = "type#声光报警";zd = new ZigbeeDevice(miao,number,type);break;

                                    case "hi":type = "type#人体红外";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    case "if":type = "type#红外对射";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    case "ga":type = "type#可燃气体";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    case "fl":type = "type#火焰气体";zd = new ZigbeeDevice(miao,number,type);zd.setCollect(true);break;
                                    default:
                                        continue;
                                }
                                number = "#number#" + result.substring(5,7);
                                emitter.onNext(new MessageInfo("link",type+"#zigbeeip#"+ipAddress + number));
                                devicelist.add(zd);//记录新设备

                            }
                        }else if (!newType.equals(temp.getEnglishType())){//如果zigbee类型不一样，则表示有人手动更换了传输类型，但是编号还是原来的编号
                            temp.setEnglishType(newType);
                            String orignalTypeKey = ipAddress+"#"+temp.getType().split("#")[1]+number;//旧的类型的键，MainUIActivity中nodeLayoutMap集合
                            String middleType = "";
                            switch (newType){
                                case "wp":middleType = "type#水泵";break;
                                case "af":middleType = "type#风扇";break;
                                case "rs":middleType = "type#卷帘";break;
                                case "pl":middleType = "type#植物生长灯";break;
                                case "cr":middleType = "type#加热器";break;
                                case "hr":middleType = "type#加湿器";break;
                                case "he":middleType = "type#温湿度";break;
                                case "ie":middleType = "type#光照度";break;
                                case "on":middleType = "type#氧气";break;
                                case "bp":middleType = "type#大气压";break;
                                case "cd":middleType = "type#二氧化碳";break;
                                case "du":middleType = "type#粉尘";break;
                                /**cssf新增应用**/
                                case "el":middleType = "type#电磁锁";break;
                                case "al":middleType = "type#可调灯";break;
                                case "re":middleType = "type#继电器";break;
                                case "or":middleType = "type#全向红外";break;
                                case "sl":middleType = "type#声光报警";break;

                                case "hi":middleType = "type#人体红外";break;
                                case "if":middleType = "type#红外对射";break;
                                case "ga":middleType = "type#可燃气体";break;
                                case "fl":middleType = "type#火焰气体";break;
                            }
                            String newTypeKey = ipAddress+"#"+middleType.split("#")[1]+number;//新的类型的键，MainUIActivity中nodeLayoutMap集合
                            emitter.onNext(new MessageInfo("typeChange",orignalTypeKey+"&"+newTypeKey+"&"+middleType.split("#")[1]));
                        }

                        //处理返回数据
//                        if(temp.isCollect()){       //只有采集传感器菜会返回数据，控制传感器不返回数据
                            emitter.onNext(new MessageInfo("data",result));
//                        }
                    }
                }
            }
        } catch (IOException e) {
//            e.printStackTrace();
        }
        //通知主线程
        emitter.onNext(new MessageInfo("exit",this.area+"#"+ipAddress));
    }

    //检测是否已经存在该zigbee节点设备
    private ZigbeeDevice findReadyExit(String number, int miao, Emitter<MessageInfo> emitter) {
        ZigbeeDevice reTemp = null;
        boolean ishasOffLine = false;
        String outList = "";        //记录掉线的序号
        for (ZigbeeDevice temp:devicelist){
            if (temp.getNumber().equals(number)){
                temp.setDateTime(miao);
                reTemp = temp;
                if (!reTemp.isOnline()){
                    reTemp.setOnline(true);
                    emitter.onNext(new MessageInfo("link",reTemp.getType()+"#zigbeeip#"+ipAddress + "#number#" + number));//重新连线
                }
            }
            if (miao - temp.getDateTime() > 10){//zigbee设备超过10秒，认定是掉线
                Log.d(TAG, "超过10秒没有收到数据: "+temp.getType());
                ishasOffLine = true;
                temp.setOnline(false);
                outList += temp.getNumber() + "#";
            }
        }
        if (ishasOffLine){
            String[] oo = outList.split("#");
            ArrayList<Integer> array = new ArrayList<Integer>();
            for (String ss : oo){
                for(int i =0;i<devicelist.size();i++){
                    if (devicelist.get(i).getNumber().equals(ss)){
                        array.add(i);
                        break;
                    }
                }
            }
            for (int i : array){
                devicelist.remove(i);
            }
            emitter.onNext(new MessageInfo("exitOne",outList));//掉线
        }
        return reTemp;
    }

    //告知主线程ipAndType
    @Override
    public void informMainIpAndType(String ipAndType){
        emitter.onNext(new MessageInfo("inform",ipAndType));
    }

    public class ZigbeeDevice{
        private int dateTime;//秒
        private String number;
        private boolean online = true;//是否在线
        private boolean collect = false; //是否是采集传感器
        private String type;
        private String englishType;

        public String getEnglishType() {
            return englishType;
        }

        public void setEnglishType(String englishType) {
            this.englishType = englishType;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isCollect() {
            return collect;
        }

        public void setCollect(boolean collect) {
            this.collect = collect;
        }

        public int getDateTime() {
            return dateTime;
        }

        public boolean isOnline() {
            return online;
        }

        public void setOnline(boolean online) {
            this.online = online;
        }

        public void setDateTime(int dateTime) {
            this.dateTime = dateTime;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public ZigbeeDevice(int dateTime, String number, String type) {
            this.dateTime = dateTime;
            this.number = number;
            this.type = type;
            switch (type){
                case "type#水泵":this.englishType = "wp";break;
                case "type#风扇":this.englishType = "af";break;
                case "type#卷帘":this.englishType = "rs";break;
                case "type#植物生长灯":this.englishType = "pl";break;
                case "type#加热器":this.englishType = "cr";break;
                case "type#加湿器":this.englishType = "hr";break;
                case "type#温湿度":this.englishType = "he";break;
                case "type#光照度":this.englishType = "ie";break;
                case "type#氧气":this.englishType = "on";break;
                case "type#大气压":this.englishType = "bp";break;
                case "type#二氧化碳":this.englishType = "cd";break;
                case "type#粉尘":this.englishType = "du";break;
                /**cssf新增应用**/
                case "type#电磁锁":this.englishType = "el";break;
                case "type#可调灯":this.englishType = "al";break;
                case "type#继电器":this.englishType = "re";break;
                case "type#全向红外":this.englishType = "or";break;
                case "type#声光报警":this.englishType = "sl";break;

                case "type#人体红外":this.englishType = "hi";break;
                case "type#红外对射":this.englishType = "if";break;
                case "type#可燃气体":this.englishType = "ga";break;
                case "type#火焰气体":this.englishType = "fl";break;
            }
        }
    }
}
