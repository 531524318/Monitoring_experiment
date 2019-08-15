package com.flag.monitoring_experiment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.flag.monitoring_experiment.customComponent.CollectLayout;
import com.flag.monitoring_experiment.customComponent.NodeLayout;
import com.flag.monitoring_experiment.customComponent.view.CustomHorizontalProgresWithNum;
import com.flag.monitoring_experiment.tcpUtils.IOBlockedRunnable;
import com.flag.monitoring_experiment.tcpUtils.IOBlockedZigbeeRunnable;
import com.flag.monitoring_experiment.tcpUtils.MessageInfo;
import com.flag.monitoring_experiment.tcpUtils.ThreadPool;

import org.json.JSONException;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    public static boolean isStartServer = false;        //是否启动了服务
    @BindView(R.id.openlistener)
    ImageView openlistener;
    @BindView(R.id.containerLine11)
    LinearLayout containerLine11;
    @BindView(R.id.containerLine21)
    LinearLayout containerLine21;
//    @BindView(R.id.containerLine12)
//    LinearLayout containerLine12;
//    @BindView(R.id.containerLine22)
//    LinearLayout containerLine22;
    @BindView(R.id.collect11)
    LinearLayout collect11;
    @BindView(R.id.collect21)
    LinearLayout collect21;
    //    @BindView(R.id.collect12)
//    LinearLayout collect12;
//    @BindView(R.id.collect22)
//    LinearLayout collect22;
    @BindView(R.id.showNetwork)
    TextView showNetwork;
    @BindView(R.id.xietiao)
    ImageView xietiao;
    @BindView(R.id.horizontalProgress3)
    CustomHorizontalProgresWithNum progress;
    @BindView(R.id.successOpen)
    TextView successOpen;
    private Context mContext;
    private ThreadPool threadPool;          //线程池
    private ArrayList<ServerSocket> serverManager = new ArrayList<>();
    public static Map<String, IOBlockedRunnable> socketMap = Collections.synchronizedMap(new HashMap<String, IOBlockedRunnable>());    //套接字Map
    private CompositeDisposable disposableManager = new CompositeDisposable();          //通道管理器
    private Map<String, NodeLayout> nodeLayoutMap = new HashMap<>(); //键是ip#typenumber 样式，zigbee率先进来的话是#typeNumber不带ip样式
    private Map<NodeLayout, CollectLayout> collectLayoutMap = new HashMap<>();
    private Timer timerCheck = new Timer();         //定时检测网络中节点是否掉线，掉线处理

    private PrintWriter outPC;                     //往电脑端发送数据对象
    private int tableWidth;     //记录屏幕宽和高
    private int tableHeight;
    public static String ZIGBEE_IP = "192.168.1.180";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }


    @OnClick(R.id.openlistener)
        public void openlistenerMethod(){
            if (isStartServer) return;
            //检查当前IP是否对应
//        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
//        //判断wifi是否开启
//        if (!wifiManager.isWifiEnabled()) {
//            successOpen.setText("请将wifi打开！");
//            return;
//        }
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int ipAddress = wifiInfo.getIpAddress();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }).start();
            String ip = "";
            try {
                NetworkInterface ni = NetworkInterface.getByName("eth0");
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                for (; ias.hasMoreElements(); ) {
                    InetAddress ia = ias.nextElement();
                    if (ia instanceof InetAddress) {
                        if (ia.toString().equals("/192.168.1.200")) {
                            ip = "192.168.1.200";
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//        String ip = intToIp(ipAddress);
            if (!ip.equals("192.168.1.200")) {
                successOpen.setText("请将IP设置成静态地址：192.168.1.200!");
                return;
            }

            progress.setProgress(0);
            progress.setMax(100);
            final Timer timer32 = new Timer();
            timer32.schedule(new TimerTask() {
                @Override
                public void run() {
                    //实时更新进度
                    if (progress.getProgress() >= progress.getMax()) {//指定时间取消
                        handler.sendEmptyMessage(0x120);
                        timer32.cancel();
                    }
                    progress.setProgress(progress.getProgress() + 5);
                }
            }, 50, 50);
            isStartServer = !isStartServer;

            timerStart();


            //观察者模式，作为监听端口，192.168.1.200:6000是wifi采集传感器 监听地址
            Observable.create(new ObservableOnSubscribe<MessageInfo>() {

                @Override
                public void subscribe(ObservableEmitter<MessageInfo> emitter) throws Exception {
                    if (threadPool == null) {
                        threadPool = new ThreadPool(12);            //初始化线程池
                    }
                    ServerSocket serverSocket = null;
                    try {
                        serverSocket = new ServerSocket(6000, 6, InetAddress.getByName("192.168.1.200"));//连接wifi采集端口
                    } catch (BindException e) {
                        emitter.onError(e);
                    }
                    if (serverSocket == null) {
                        emitter.onComplete();
                        isStartServer = !isStartServer;     //允许再次建立监听
                        return;
                    }
                    serverManager.add(serverSocket);
                    while (true) {
                        Socket clientsocket = null;
                        try {
                            clientsocket = serverSocket.accept();
                        } catch (Exception e) {
//                        e.printStackTrace();
                            break;
                        }
                        String clientAddress = clientsocket.getInetAddress().toString().trim();
                        if (!hasClientIPExite(clientAddress)) {
                            String area = "11";//界面图标默认放置区域11
                            if (containerLine11.getChildCount() == 3) area = "21";//如果已经满了，那就换个区域21
                            IOBlockedRunnable run = new IOBlockedRunnable(clientsocket, area, emitter);
                            synchronized (socketMap) {
                                socketMap.put(clientAddress, run);

                            }
                            threadPool.execute(run);
                        } else {
                            //发现重复ip，则断开该ip原本的连接，使用新连接
                            getClientIPExite(clientAddress).closeSocket();
//                        String area = "12";
//                        if (containerLine12.getChildCount() == 3) area = "22";
//                        IOBlockedRunnable run = new IOBlockedRunnable(clientsocket, area, emitter);
//                        synchronized (socketMap) {
//                            socketMap.put(clientAddress, run);
//
//                        }
//                        threadPool.execute(run);
                            handler.sendEmptyMessage(0x123);
                        }
                    }
                }
            }).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<MessageInfo>() {
                                   private Disposable mDisposable;
                                   private CollectLayout nowCollect;

                                   @Override
                                   public void onSubscribe(Disposable d) {
                                       disposableManager.add(d);           //保存通道
                                       mDisposable = d;
                                   }

                                   @Override
                                   public void onNext(MessageInfo minfo) {
                                       switch (minfo.getInfoType()) {
                                           case "link"://连接对象
                                               String value = minfo.getContext();              //获取内容,形如：wifiip#192.168.1.1#type#温湿度#nuber#编号
                                               String[] update = value.split("#");
                                               String ip1 = update[1];
                                               String type1 = update[3];
                                               String numberLink = update[5];              //编号
                                               String ipAndTypeNumber = ip1 + "#" + type1 + numberLink;     //ip和类型序号，形如192.168.1.1#温湿度01
                                               NodeLayout nllink = wififindExiteNodeLayout(ipAndTypeNumber);//寻找是否已经存在的节点
                                               if (nllink == null) {
                                                   nllink = new NodeLayout(getApplicationContext(), null);
                                                   LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                                                   nllink.onlyIp = ipAndTypeNumber;
                                                   nllink.updateHiddenText(value);
                                                   if (containerLine11.getChildCount() < 3) {      //一行三列，超出则换一行
                                                       containerLine11.addView(nllink, lp);
                                                   } else if (containerLine21.getChildCount() < 3) {
                                                       containerLine21.addView(nllink, lp);
                                                   }
                                                   //采集传感器区域=》增加传感器
                                                   CollectLayout cl = new CollectLayout(getApplicationContext(), null);
                                                   cl.setShowInit(type1, "wifi", ip1, numberLink);
                                                   cl.setConfig(MainActivity.this, tableWidth, tableHeight);//设置屏幕参数
                                                   if (collect11.getChildCount() < 3) {
                                                       collect11.addView(cl, lp);
                                                   } else if (collect21.getChildCount() < 3) {
                                                       collect21.addView(cl, lp);
                                                   }
                                                   collectLayoutMap.put(nllink, cl);
                                                   nodeLayoutMap.put(ipAndTypeNumber, nllink);
                                               } else {

                                                   //加入事先已有zigbee链接，那么应该改变key值
                                                   nllink.updateHiddenText(value);
                                                   nllink.hideZigbeeTrue();
                                                   nodeLayoutMap.remove("#" + type1);      //首先移除原来的key，保留值
                                                   nodeLayoutMap.put(ipAndTypeNumber, nllink);     //将新键重新加入到map中
                                                   collectLayoutMap.get(nllink).changeStatueLink("wifi", ip1);//改变连接显示状态

                                               }
                                               handlerData(type1, numberLink, "linkwifi");
                                               break;
                                           case "exit"://退出对象
                                               String[] areaAndipType = minfo.getContext().split("#");
                                               String area = areaAndipType[0];
                                               String ip = areaAndipType[1];
                                               String type = areaAndipType[2];
                                               String number = areaAndipType[3];
                                               synchronized (socketMap) {
                                                   socketMap.remove(ip);
                                               }
                                               //移除显示节点，退出前检测zigbee那一块是否还在连接
                                               String ipType = ip + "#" + type + number;
                                               NodeLayout nlexit = nodeLayoutMap.get(ipType);
                                               if (nlexit == null) {

                                               } else {
                                                   if (nodeLayoutMap.get(ipType).hideWifi() == 2) {         //返回整数2，表示要移除整个组件
                                                       NodeLayout remove = nodeLayoutMap.remove(ipType);
                                                       CollectLayout cl = collectLayoutMap.remove(remove);                    //采集map集合也要移除
                                                       cl.closeUIPopWindow();
                                                       switch (area) {
                                                           case "11":
                                                               containerLine11.removeView(remove);
                                                               collect11.removeView(cl);
                                                               break;
                                                           case "21":
                                                               containerLine21.removeView(remove);
                                                               collect21.removeView(cl);
                                                               break;
                                                       }
                                                   } else {
                                                       nodeLayoutMap.get(ipType).updateHiddenText("wifiip##wifistatue#false");
                                                       nodeLayoutMap.get(ipType).hideWifiTrue();
                                                       //更新传感器连接状态
                                                       try {
                                                           collectLayoutMap.get(nlexit).changeStatueLink("zigbee", (String) nodeLayoutMap.get(ipType).getHiddenTextJSON().get("zigbeeip"));
                                                       } catch (JSONException e) {
                                                           e.printStackTrace();
                                                       }
                                                   }
                                               }
                                               handlerData(type, number, "exitWifi");
                                               break;
                                           case "inform":      //知会主线程，是哪一个采集传感器
                                               String keyValue = minfo.getContext();
                                               if (!keyValue.equals("")) {
                                                   nowCollect = collectLayoutMap.get(nodeLayoutMap.get(keyValue));
                                               } else {
                                                   nowCollect = null;              //当前keyValue 是空字符串时表示关闭了该曲线图对话框页面
                                               }
                                               break;
                                           case "data":        //数据信息
                                               String dataContext = minfo.getContext();              //获取内容
                                               //解析数据，得出传感器采集到的值
                                               if (nowCollect != null && nowCollect.getNumber().equals(dataContext.substring(5, 7))) {
                                                   //是否显示折线图
                                                   nowCollect.updateChartData(dataContext);
                                               }
                                               //是否将数据传递给外网服务器或者是局域网服务器
//                                           if (SetCallSerialFragment.startSend) {
//                                               if (SetCallSerialFragment.isSendToServer) {
//                                                   SetCallSerialFragment.phoneCall.printWriter.write("Htcpsend" + dataContext);
//                                                   SetCallSerialFragment.phoneCall.printWriter.write(0x1a);
//                                                   SetCallSerialFragment.phoneCall.printWriter.flush();
//                                               }
//                                               if (SetCallSerialFragment.isLinkLocalServer) {//是否往局域网客户端发送数据
//                                                   try {
//                                                       SetCallSerialFragment.outLocal.write(dataContext.getBytes());
//                                                   } catch (IOException e) {
//                                                       e.printStackTrace();
//                                                   }
//                                               }
//                                           }
                                               String numberData = dataContext.substring(5, 7);//编号
                                               switch (dataContext.substring(3, 5)) {
                                                   case "he":
                                                       numberData = "温湿度" + numberData;
                                                       break;
                                                   case "ie":
                                                       numberData = "光照度" + numberData;
                                                       break;
                                                   /**cssf新增应用**/
                                                   case "hi":
                                                       numberData = "人体红外" + numberData;
                                                       break;
                                                   case "if":
                                                       numberData = "红外对射" + numberData;
                                                       break;
                                                   case "ga":
                                                       numberData = "可燃气体" + numberData;
                                                       break;
                                                   case "fl":
                                                       numberData = "火焰气体" + numberData;
                                                       break;
                                                   default:
                                                       break;
                                               }
                                               //假如此时是zigbee连接显示，应该该为wifi连接显示
                                               NodeLayout upnode = wififindExiteNodeLayout("#" + numberData);//#Type number
                                               if (upnode != null) {
                                                   if (upnode.getLinkType().equals("zigbee")) {
                                                       upnode.hideZigbeeTrue();
                                                       //更新传感器连接网络方式
                                                       String ipWIFI = "";
                                                       try {
                                                           ipWIFI = upnode.getHiddenTextJSON().getString("wifiip");
                                                       } catch (JSONException e) {
                                                           e.printStackTrace();
                                                       }
                                                       collectLayoutMap.get(upnode).changeStatueLinkNoPOP("wifi", ipWIFI);
                                                   }
                                               }

                                               //20190731增加报警系统相关的功能
//                                           AlarmService.getInstance().handleRecvData(dataContext, mContext, nodeLayoutMap, socketMap);

                                               writeMessageToPC(dataContext);//发送信息给PC客户端
                                               break;
                                           case "typeChange":
                                               String[] typeChangeKey = minfo.getContext().split("&");//typeChangeKey[0]是旧类型的键，typeChangeKey[1]是新类型的键,typeChangeKey[2]是中文类型
                                               //下面这些顺序不能乱，乱了程序就错了
                                               String ipNew = typeChangeKey[0].split("#")[0];
                                               String numberNew = typeChangeKey[0].substring(typeChangeKey[0].length() - 2);
                                               NodeLayout orignalNode = nodeLayoutMap.get(typeChangeKey[0]);
                                               CollectLayout orignalColle = collectLayoutMap.get(orignalNode);
                                               orignalColle.setShowInit(typeChangeKey[2], "wifi", ipNew, numberNew);
                                               collectLayoutMap.remove(orignalColle);                   //移除
                                               orignalNode.onlyIp = typeChangeKey[1];                  //采集节点作出改变
                                               orignalNode.updateHiddenText("type#" + typeChangeKey[2]);//改变隐藏域内容
                                               nodeLayoutMap.put(typeChangeKey[1], orignalNode);         //将新键值存进去
                                               collectLayoutMap.put(orignalNode, orignalColle);
                                               handlerData(typeChangeKey[2], numberNew, "linkChange");
                                               break;
                                       }
                                   }

                                   @Override
                                   public void onError(Throwable e) {
                                       e.printStackTrace();
                                   }

                                   @Override
                                   public void onComplete() {
                                       successOpen.setText("打开失败，端口被占用！\n" + "请重启该软件！");
                                       mDisposable.dispose();
                                       closeListener();
                                   }
                               }
                    );
            //观察者模式，作为监听端口，192.168.1.200:6002是zigbee 收集传感器数据和控制设备
            Observable.create(new ObservableOnSubscribe<MessageInfo>() {
                @Override
                public void subscribe(ObservableEmitter<MessageInfo> emitter) throws Exception {
                    if (threadPool == null) {
                        threadPool = new ThreadPool(12);            //初始化线程池
                    }
                    ServerSocket serverSocket = null;
                    try {
                        serverSocket = new ServerSocket(6002, 6, InetAddress.getByName("192.168.1.200"));//连接zigbee端口
                    } catch (Exception e) {
                        emitter.onError(e);
                    }
                    if (serverSocket == null) {
                        isStartServer = !isStartServer;     //允许再次建立监听
                        emitter.onComplete();
                        return;
                    }
                    serverManager.add(serverSocket);
                    while (true) {
                        Socket clientsocket = null;
                        try {
                            clientsocket = serverSocket.accept();
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                        try {
                            String clientAddress = clientsocket.getInetAddress().toString().trim();
                            //发现重复ip，则断开该ip原本的连接，使用新连接
                            if (hasClientIPExite(clientAddress)) {
                                getClientIPExite(clientAddress).closeSocket();
                                handler.sendEmptyMessage(0x123);
                            }
                            String area = "";
                            final IOBlockedZigbeeRunnable run = new IOBlockedZigbeeRunnable(clientsocket, area, emitter);
                            synchronized (socketMap) {
                                socketMap.put(clientAddress, run);
                            }
                            threadPool.execute(run);
                            Message msgZigLink = new Message();
                            msgZigLink.what = 0x121;
                            msgZigLink.obj = clientAddress + ":" + clientsocket.getPort();
                            handler.sendMessage(msgZigLink);

                        } catch (Exception e) {
                        }
                    }
                }
            }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<MessageInfo>() {
                        private Disposable mDisposable;
                        private CollectLayout nowCollect;

                        @Override
                        public void onSubscribe(Disposable d) {
                            mDisposable = d;
                            disposableManager.add(d);
                        }

                        @Override
                        public void onNext(MessageInfo minfo) {
                            switch (minfo.getInfoType()) {
                                case "link":
                                    String orignal = minfo.getContext();
                                    String[] data = orignal.split("#");
                                    String type = data[1];
                                    String ipZigbee = data[3];
                                    String numberLink = data[5];              //编号
                                    NodeLayout nllink = zigbeefindExiteNodeLayout(type + numberLink);
                                    if (nllink == null) {
                                        nllink = new NodeLayout(getApplicationContext(), null);
                                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                                        nllink.onlyIp = "#" + type + numberLink;//这里之所以不添加ipZigbee 作为键，是因为考虑到同时存在wifi和zigbee节点共用一个传感器，那么ip地址应该填的是wifi的ip
                                        nllink.updateHiddenText(orignal);
                                        nllink.setDefaultType();
                                        CollectLayout cl = new CollectLayout(getApplicationContext(), null);
                                        cl.setShowInit(type, "zigbee", ipZigbee, numberLink);
                                        cl.setConfig(MainActivity.this, tableWidth, tableHeight);//设置屏幕参数
                                        //根据类型判断是控制传感器还是采集传感器
                                        boolean control = false;
                                        switch (type) {
                                            case "水泵":
                                            case "风扇":
                                            case "卷帘":
                                            case "植物生长灯":
                                            case "加热器":
                                            case "加湿器":
                                                /**cssf新增应用**/
                                            case "电磁锁":
                                            case "可调灯":
                                            case "继电器":
                                            case "全向红外":
                                            case "声光报警":
                                                control = true;
                                                break;
                                        }
                                        if (control) {                               //放入右边
                                            nllink.setImageBackgroundCanClick();
//                                        if (containerLine12.getChildCount() < 3) {      //一行三列，超出则换一行
//                                            containerLine12.addView(nllink, lp);
//                                        } else if (containerLine22.getChildCount() < 3) {
//                                            containerLine22.addView(nllink, lp);
//                                        }
//                                        if (collect12.getChildCount() < 3) {
//                                            collect12.addView(cl, lp);
//                                        } else if (collect22.getChildCount() < 3) {
//                                            collect22.addView(cl, lp);
//                                        }
//                                    } else {                                      //放入左边
                                            if (containerLine11.getChildCount() < 3) {      //一行三列，超出则换一行
                                                containerLine11.addView(nllink, lp);
                                            } else if (containerLine21.getChildCount() < 3) {
                                                containerLine21.addView(nllink, lp);
                                            }
                                            if (collect11.getChildCount() < 3) {
                                                collect11.addView(cl, lp);
                                            } else if (collect21.getChildCount() < 3) {
                                                collect21.addView(cl, lp);
                                            }
                                        }
                                        collectLayoutMap.put(nllink, cl);
                                        nodeLayoutMap.put(nllink.onlyIp, nllink);
                                    } else {
                                        nllink.hideWifiTrue();
                                        nllink.updateHiddenText(orignal);
                                        //更新传感器连接网络方式
                                        collectLayoutMap.get(nllink).changeStatueLink("zigbee", "/" + ZIGBEE_IP);
                                    }
                                    handlerData(nllink.getSensorType(), numberLink, "linkzigbee");
                                    break;
                                case "exit":
                                    String[] orignalZ = minfo.getContext().split("#");
                                    String ip = orignalZ[1];
                                    synchronized (socketMap) {
                                        socketMap.remove(ip);
                                    }
                                    Iterator<String> it = nodeLayoutMap.keySet().iterator();
                                    ArrayList<String> outList = new ArrayList<String>();
                                    while (it.hasNext()) {  //因为zigbee中一个连接地址包含多个节点，如果这个连接断开了，那么势必其下所有节点断开，所以我这里使用遍历map移除
                                        String removestr = it.next();   //键是ip#typenumber 样式，zigbee率先进来的话是#typeNumber不带ip样式
                                        NodeLayout remove = nodeLayoutMap.get(removestr);
                                        if (remove.hideZigbee() == 2) {
//                                        remove.closeNode();
                                            outList.add(removestr);
                                            CollectLayout cl = collectLayoutMap.remove(remove); //采集map集合也要移除
                                            if (cl == null)
                                                continue;
                                            containerLine11.removeView(remove);
                                            containerLine21.removeView(remove);
//                                        containerLine12.removeView(remove);
//                                        containerLine22.removeView(remove);

                                            cl.closeUIPopWindow();
                                            collect11.removeView(cl);
                                            collect21.removeView(cl);
//                                        collect12.removeView(cl);
//                                        collect22.removeView(cl);
                                        } else {

                                            nodeLayoutMap.get(removestr).updateHiddenText("zigbeeip##zigbeestatue#false");
                                            nodeLayoutMap.get(removestr).hideZigbeeTrue();
                                            //更新传感器连接状态
                                            try {
                                                if (collectLayoutMap.get(remove) != null)
                                                    collectLayoutMap.get(remove).changeStatueLink("wifi", (String) nodeLayoutMap.get(removestr).getHiddenTextJSON().get("wifiip"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                    for (String outone : outList) {
//                                    nodeLayoutMap.remove(outone).closeNode();
                                    }
                                    handler.sendEmptyMessage(0x122);

                                    break;
                                case "exitOne":                 //仅仅是单个节点退出网络
                                    String[] numberexit = minfo.getContext().split("#");//形如：01#02#
                                    Iterator<String> itOrg = nodeLayoutMap.keySet().iterator();
                                    int lenLess = 0;
                                    ArrayList<String> list = new ArrayList<String>();
                                    while (itOrg.hasNext()) {
                                        String removestr = itOrg.next();
                                        String checkStr = removestr.split("#")[1];
                                        for (String strout : numberexit) {
                                            if (checkStr.contains(strout)) {    //键包含了序号的表示掉线的，移除
                                                String offType = removestr.substring(removestr.indexOf("#") + 1, removestr.length() - 2);///192.168.1.100#温湿度01
                                                String offnumber = removestr.substring(removestr.length() - 2);
                                                handlerData(offType, offnumber, "outoff");
                                                NodeLayout remove = nodeLayoutMap.get(removestr);
                                                if (remove.hideZigbee() == 2) {
//                                                itOrg.remove();
                                                    list.add(removestr);

                                                    containerLine11.removeView(remove);
                                                    containerLine21.removeView(remove);
//                                                containerLine12.removeView(remove);
//                                                containerLine22.removeView(remove);
                                                    CollectLayout cl = collectLayoutMap.remove(remove); //采集map集合也要移除
                                                    if (cl == null) continue;
                                                    cl.closeUIPopWindow();
                                                    collect11.removeView(cl);
                                                    collect21.removeView(cl);
//                                                collect12.removeView(cl);
//                                                collect22.removeView(cl);
                                                } else {
                                                    nodeLayoutMap.get(removestr).updateHiddenText("zigbeeip##zigbeestatue#false");
                                                    nodeLayoutMap.get(removestr).hideZigbeeTrue();
                                                    //更新传感器连接状态
                                                    try {
                                                        collectLayoutMap.get(remove).changeStatueLink("wifi", (String) nodeLayoutMap.get(removestr).getHiddenTextJSON().get("wifiip"));
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                handlerData(remove.getSensorType(), strout, "exit");
                                                lenLess++;
                                                if (lenLess == numberexit.length)
                                                    break;//已经检测完毕，下面的不需要再遍历

                                            }
                                        }
                                    }
                                    for (String one : list) {
//                                    nodeLayoutMap.remove(one).closeNode();
                                    }

                                    break;
                                case "inform":      //知会主线程，是哪一个采集传感器
                                    String orignalZigbee = minfo.getContext();
                                    if (!orignalZigbee.equals("")) {
                                        nowCollect = collectLayoutMap.get(nodeLayoutMap.get("#" + orignalZigbee.split("#")[1]));//首先在nodeLayoutMap中寻找是否存在
                                        if (nowCollect == null) {
                                            for (String keyValue : nodeLayoutMap.keySet()) {
                                                if (keyValue.contains(orignalZigbee.split("#")[1])) {
                                                    nowCollect = collectLayoutMap.get(nodeLayoutMap.get(keyValue));
                                                    break;
                                                }
                                            }
                                        }
                                    } else {
                                        nowCollect = null;
                                    }
                                    break;
                                case "data"://数据对象
                                    String dataContext = minfo.getContext();              //获取内容
                                    //解析数据，得出传感器采集到的值

                                    if (nowCollect != null && nowCollect.getNumber().equals(dataContext.substring(5, 7))) {
                                        nowCollect.updateChartData(dataContext);
                                    }
                                    //将数据传递给外网服务器和局域网服务器
//                                if (SetCallSerialFragment.startSend) {       //是否传送数据
//                                    if (SetCallSerialFragment.isSendToServer) {
//                                        sbSendDate.append(dataContext);
//                                        sbSendDate.append("\r\n");
//                                        countsbSendDate++;
//                                        if (countsbSendDate == 6) {
//                                            countsbSendDate = 0;
//                                            String send = sbSendDate.toString();
//                                            sbSendDate.setLength(0);
//                                            SetCallSerialFragment.phoneCall.printWriter.write("Htcpsend" + send);
//                                            SetCallSerialFragment.phoneCall.printWriter.write(0x1a);
//                                            SetCallSerialFragment.phoneCall.printWriter.flush();
//                                        }
////                                        Log.d(TAG, "发送给阿里云一次: ");
//                                    }
//                                    if (SetCallSerialFragment.isLinkLocalServer) {
//                                        try {
//                                            SetCallSerialFragment.outLocal.write(dataContext.getBytes());
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                }
                                    String numberData = dataContext.substring(5, 7);//编号
                                    switch (dataContext.substring(3, 5)) {
                                        case "he":
                                            numberData = "温湿度" + numberData;
                                            break;
                                        case "ie":
                                            numberData = "光照度" + numberData;
                                            break;
                                        case "on":
                                            numberData = "氧气" + numberData;
                                            break;
                                        case "bp":
                                            numberData = "大气压" + numberData;
                                            break;
                                        case "cd":
                                            numberData = "二氧化碳" + numberData;
                                            break;
                                        case "du":
                                            numberData = "粉尘" + numberData;
                                            break;

                                        case "wp":
                                            numberData = "水泵" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "af":
                                            numberData = "风扇" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "rs":
                                            numberData = "卷帘" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "pl":
                                            numberData = "植物生长灯" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "cr":
                                            numberData = "加热器" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "hr":
                                            numberData = "加湿器" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;

                                        /**cssf新增应用**/
                                        case "el":
                                            numberData = "电磁锁" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "al":
                                            numberData = "可调灯" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "re":
                                            numberData = "继电器" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "or":
                                            numberData = "全向红外" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "sl":
                                            numberData = "声光报警" + numberData;
                                            changeSwitchStatus(dataContext, numberData);
                                            break;
                                        case "hi":
                                            numberData = "人体红外" + numberData;
                                            break;
                                        case "if":
                                            numberData = "红外对射" + numberData;
                                            break;
                                        case "ga":
                                            numberData = "可燃气体" + numberData;
                                            break;
                                        case "fl":
                                            numberData = "火焰气体" + numberData;
                                            break;
                                        default:
                                            break;
                                    }
                                    //判断此时连接状态，如果这时wifi自动更新为zigbee,我这里假设是wifi连接那么key有ip值
                                    NodeLayout datanode = zigbeefindExiteNodeLayout(numberData);
                                    if (datanode != null) {
                                        if (datanode.getLinkType().equals("wifi")) {
                                            datanode.hideWifiTrue();
                                            datanode.updateHiddenText("zigbeeip#" + ZIGBEE_IP + "#zigbeestatue#true");
                                            //更新传感器连接网络方式
                                            collectLayoutMap.get(datanode).changeStatueLinkNoPOP("zigbee", ZIGBEE_IP);
                                        }
                                    }
                                    writeMessageToPC(dataContext);
                                    break;
                                case "typeChange":
                                    String[] typeChangeKey = minfo.getContext().split("&");//typeChangeKey[0]是旧类型的键，typeChangeKey[1]是新类型的键,typeChangeKey[2]是中文类型
                                    //下面这些顺序不能乱，乱了程序就错了
                                    String ipNew = typeChangeKey[0].split("#")[0];
                                    String numberNew = typeChangeKey[0].substring(typeChangeKey[0].length() - 2);
                                    String maybeKey = typeChangeKey[0].split("#")[1];
                                    String newKey = "#" + typeChangeKey[1].split("#")[1];
                                    NodeLayout orignalNode = null;
                                    String orignalKey = "";
                                    for (String temp : nodeLayoutMap.keySet()) {        //遍历寻找该Node节点
                                        if (temp.contains(maybeKey)) {
                                            orignalKey = temp;
                                            orignalNode = nodeLayoutMap.get(temp);
                                        }
                                    }
                                    if (orignalNode == null) break;
                                    CollectLayout orignalColle = collectLayoutMap.get(orignalNode);
                                    orignalColle.setShowInit(typeChangeKey[2], "zigbee", ipNew, numberNew);
                                    collectLayoutMap.remove(orignalColle);                   //移除
                                    if (!orignalKey.split("#")[0].equals("")) {
                                        newKey = orignalKey.split("#")[0] + newKey;
                                    }
                                    orignalNode.onlyIp = newKey;                  //采集节点作出改变
                                    orignalNode.updateHiddenText("type#" + typeChangeKey[2]);//改变隐藏域内容

//                                nodeLayoutMap.remove(orignalKey).closeNode();                 //移除原本的键值
                                    nodeLayoutMap.put(newKey, orignalNode);         //将新键值存进去
                                    collectLayoutMap.put(orignalNode, orignalColle);
                                    handlerData(typeChangeKey[2], numberNew, "linkChange");
                                    break;
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onComplete() {
                            successOpen.setText("打开失败，端口被占用！\n请重启该软件！");
                            mDisposable.dispose();
                            closeListener();
                        }
                    });
        }
    //定时检查runable 的数据传输以及连接状态
    private void timerStart() {
        timerCheck.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    ArrayList<String> listOut = new ArrayList();
                    synchronized (socketMap) {
                        for (IOBlockedRunnable temp : socketMap.values()) {
                            SimpleDateFormat df = new SimpleDateFormat("mm:ss");
                            String timestr = df.format(new Date().getTime());
                            String[] times = timestr.split(":");
                            int miao = Integer.parseInt(times[0].trim()) * 60 + Integer.parseInt(times[1].trim());
                            String ipAddd = temp.getSocket().getInetAddress().toString().substring(1);
                            int nowmiao = temp.getNowTime();

                            if (ipAddd.equals(ZIGBEE_IP)) {//这个是转换器的IP地址
                                handler.sendEmptyMessage(0x211);
                                if (Math.abs(miao - temp.getNowTime()) > 30) {//三十秒没收到数据就判定真个zigbee通道掉线
                                    temp.closeSocket();
                                }
                                continue;
                            }
                            if (Math.abs(miao - nowmiao) > 12) {
//                                Log.d(TAG, "run: 超过12秒没有接受到数据" + ipAddd);
                                Process p2 = Runtime.getRuntime().exec("ping -c 1 -w 1 " + ipAddd);
                                int status2 = p2.waitFor(); // PING的状态
                                if (status2 == 0) {          //等于零表示能ping 得通，不作任何处理,否则不在线 需要作掉线处理

                                } else {

                                    String keyIP = "/" + ipAddd;
                                    listOut.add(keyIP);
                                    //移除显示节点，退出前检测zigbee那一块是否还在连接
                                    if (temp instanceof IOBlockedZigbeeRunnable) {

                                        Iterator<String> it = nodeLayoutMap.keySet().iterator();
                                        ArrayList<String> nodeOut = new ArrayList<String>();
                                        while (it.hasNext()) {  //因为zigbee中一个连接地址包含多个节点，如果这个连接断开了，那么势必其下所有节点断开，所以我这里使用遍历map移除
                                            String removestr = it.next();
                                            NodeLayout remove = nodeLayoutMap.get(removestr);
                                            if (remove.hideZigbee() == 2) {
                                                nodeOut.add(removestr);
                                                Message msgZgRM = new Message();
                                                msgZgRM.what = 0x127;
                                                msgZgRM.obj = removestr;
                                                handler.sendMessage(msgZgRM);
                                            } else {
                                                Message msgZgRM = new Message();
                                                msgZgRM.what = 0x127;
                                                msgZgRM.obj = removestr;
                                                handler.sendMessage(msgZgRM);
                                            }
                                        }
                                        for (String nodeTemp : nodeOut) {
//                                            nodeLayoutMap.remove(nodeTemp).closeNode();
                                        }
                                        handler.sendEmptyMessage(0x122);
                                    } else {

                                        String type = "", number = "";
                                        type = temp.getType();
                                        number = temp.getNumber();

                                        if (type.equals("") || number.equals("") || type == null || number == null) {

                                            listOut.add(keyIP);
                                            continue;
                                        }
                                        String ipType = keyIP + "#" + type.split("#")[1] + number;
                                        Message msgRemove = new Message();
                                        msgRemove.what = 0x126;
                                        msgRemove.obj = ipType + "&" + temp.getArea();
                                        handler.sendMessage(msgRemove);
                                        handlerData(type.split("#")[1], number, "exit");
                                    }
                                }
                            }
                        }
                        for (String temp : listOut) {
                            IOBlockedRunnable iorun = socketMap.remove(temp);
                            if (iorun != null)
                                iorun.closeSocket();
                        }

                    }
                } catch (Exception e) {

                    System.gc();
                    e.printStackTrace();

                }
            }
        }, 10000, 10000);
    }
    private boolean hasClientIPExite(String clientAddress) {
        if (socketMap.containsKey(clientAddress))
            return true;
        return false;
    }
    private IOBlockedRunnable getClientIPExite(String clientAddress) {
        if (socketMap.containsKey(clientAddress))
            return socketMap.get(clientAddress);
        return null;
    }
    private NodeLayout wififindExiteNodeLayout(String ipAndType) {//寻找NodeLayoutMap集合中已经存在的nodeLayout
        String[] type = ipAndType.split("#");
        for (String temp : nodeLayoutMap.keySet()) {
            if (type[1].equals(temp.split("#")[1])) {             //专门检查是否存在该类型，即 type
                return nodeLayoutMap.get(temp);
            }
        }
        return null;
    }
    //关闭所有serversocket
    public void closeAllServerSockert() {
        for (ServerSocket temp : serverManager) {
            try {
                if (temp != null && !temp.isClosed()) {
                    temp.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        serverManager.clear();
    }
    //关闭监听管理和serversocket
    public void closeListener() {
        disposableManager.clear();
        closeAllServerSockert();
    }
    //将信息发送给C#软件
    private void writeMessageToPC(String s) {
        if (outPC != null) {
            outPC.write(s);
            outPC.flush();
        }
    }
    private void handlerData(String type1, String numberLink, String linkOrExit) {
        Message msgLink = new Message();
        msgLink.what = 0x125;
        switch (linkOrExit) {
            case "linkwifi":
                msgLink.obj = "编号为" + numberLink + "的" + type1 + "wifi节点连入网络";
                break;
            case "linkzigbee":
                msgLink.obj = "编号为" + numberLink + "的" + type1 + "zigbee节点连入网络";
                break;
            case "exit":
                msgLink.obj = "编号为" + numberLink + "的" + type1 + "节点退出网络";
                break;
            case "cshar":
                //msgLink.obj = "电脑客户端上位机软件连入网络";
                msgLink.obj = "客户端软件连入网络";
                break;
            case "linkChange":
                msgLink.obj = "编号为" + numberLink + "的类型自动更改为" + type1;
                break;
            case "outoff":
                msgLink.obj = "编号为" + numberLink + "的" + type1 + "zigbee节点超过10秒未收到数据!!!";
                break;
            case "exitWifi":
                msgLink.obj = "编号为" + numberLink + "的" + type1 + "wifi节点退出网络";
                break;
            case "exitZigbee":
                msgLink.obj = "所有zigbee节点退出网络";
                break;
        }
        handler.sendMessage(msgLink);
    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x120:             //更新路由器的颜色，false表示连接成功
                    if (isStartServer) {
                        openlistener.setImageResource(R.drawable.routertrue);
                        progress.setVisibility(View.GONE);
                    } else {
                        openlistener.setImageResource(R.drawable.routerpress);
                        progress.setVisibility(View.VISIBLE);
                    }
                    break;
                case 0x121:             //更新转换器和协调器的颜色表示工作进行
                    xietiao.setImageResource(R.drawable.xietiaotrue);
                    String mm = msg.obj.toString();
                    Message msgLink = new Message();
                    msgLink.what = 0x125;
                    msgLink.obj = "zigbee连入网络！" + mm;
                    handler.sendMessage(msgLink);
                    break;
                case 0x211:             //更新转换器颜色表示工作进行
                    break;
                case 0x122:             //表示协调器和转换器暂时不工作
                    xietiao.setImageResource(R.drawable.xietiaofalse);
                    handlerData("", "", "exitZigbee");
                    break;
                case 0x123:
                    Toast.makeText(mContext, "连接的wifi节点与已存在的IP产生冲突，替换旧连接", Toast.LENGTH_LONG).show();
                    break;
                case 0x124:
                    Toast.makeText(mContext, "该端口已经存在一个连接,只允许一个连接", Toast.LENGTH_LONG).show();
                    break;
                case 0x125:
                    //设置滚动文本框
                    if (msg.obj == null) break;
                    if (showNetwork != null) {
                        showNetwork.append(msg.obj.toString() + "\n");
                        int offset = showNetwork.getLineCount() * showNetwork.getLineHeight();
                        if (offset > (showNetwork.getHeight() - showNetwork.getLineHeight() - 5)) {
                            showNetwork.scrollTo(0, offset - showNetwork.getHeight() + showNetwork.getLineHeight() + 5);
                        }
                        if (showNetwork.getLineCount() > 15) {
                            showNetwork.setText("");
                            showNetwork.scrollTo(0, 0);
                        }
                    }
                    break;
                case 0x126:                 //界面上移除wifi节点图形
                    String[] rm = msg.obj.toString().split("&");
                    String ipType = rm[0];
                    String area = rm[1];
                    NodeLayout nlexit = nodeLayoutMap.get(ipType);
                    if (nlexit == null) break;
                    if (nodeLayoutMap.get(ipType).hideWifi() == 2) { //返回整数2，表示要移除整个组件
                        NodeLayout remove = nodeLayoutMap.remove(ipType);
//                        remove.closeNode();
                        CollectLayout cl = collectLayoutMap.remove(remove);                    //采集map集合也要移除
                        cl.closeUIPopWindow();
                        collect11.removeView(cl);
                        collect21.removeView(cl);
//                        collect12.removeView(cl);
//                        collect22.removeView(cl);
                        switch (area) {
                            case "11":
                                containerLine11.removeView(remove);
                                break;
                            case "21":
                                containerLine21.removeView(remove);
                                break;
                            case "12":
//                                containerLine12.removeView(remove);
                                break;
                            case "22":
//                                containerLine22.removeView(remove);
                                break;
                        }
                    } else {
                        nodeLayoutMap.get(ipType).updateHiddenText("wifiip##wifistatue#false");
                        nodeLayoutMap.get(ipType).hideWifiTrue();
                        try {
                            collectLayoutMap.get(nlexit).changeStatueLink("zigbee", (String) nodeLayoutMap.get(ipType).getHiddenTextJSON().get("zigbeeip"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 0x127:                 //界面上移除zigbee节点图形
                    String removestr = msg.obj.toString();
                    NodeLayout remove = nodeLayoutMap.get(removestr);
                    if (remove.hideZigbee() == 2) {
//                        nodeLayoutMap.remove(removestr).closeNode();
                        containerLine11.removeView(remove);
                        containerLine21.removeView(remove);
//                        containerLine12.removeView(remove);
//                        containerLine22.removeView(remove);
                        CollectLayout cl = collectLayoutMap.remove(remove); //采集map集合也要移除
                        cl.closeUIPopWindow();
                        collect11.removeView(cl);
                        collect21.removeView(cl);
//                        collect12.removeView(cl);
//                        collect22.removeView(cl);
                    } else {
                        nodeLayoutMap.get(removestr).updateHiddenText("zigbeeip##zigbeestatue#false");
                        nodeLayoutMap.get(removestr).hideZigbeeTrue();
                        //更新传感器连接状态
                        try {
                            collectLayoutMap.get(remove).changeStatueLink("wifi", (String) nodeLayoutMap.get(removestr).getHiddenTextJSON().get("wifiip"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        }
    };
    private NodeLayout zigbeefindExiteNodeLayout(String s) {//寻找NodeLayoutMap集合中已经存在的nodeLayout
        for (String temp : nodeLayoutMap.keySet()) {
            if (s.equals(temp.split("#")[1])) {
                return nodeLayoutMap.get(temp);
            }
        }
        return null;
    }
    //改变开关状态
    private void changeSwitchStatus(String dataContext, String numberData) {
        for (String dataKey : nodeLayoutMap.keySet()) {
            if (dataKey.contains(numberData)) {
                NodeLayout middle = nodeLayoutMap.get(dataKey);
                middle.setStatueControl(dataContext);
                break;
            }
        }
    }
}


