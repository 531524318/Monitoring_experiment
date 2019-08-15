package com.flag.monitoring_experiment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.flag.monitoring_experiment.customComponent.CollectLayout;
import com.flag.monitoring_experiment.customComponent.NodeLayout;
import com.flag.monitoring_experiment.tcpUtils.IOBlockedRunnable;
import com.flag.monitoring_experiment.tcpUtils.MessageInfo;
import com.flag.monitoring_experiment.tcpUtils.ThreadPool;

import org.json.JSONException;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
@BindView(R.id.successOpen)
TextView successOpen;
    private ThreadPool threadPool;          //线程池
    private ArrayList<ServerSocket> serverManager = new ArrayList<>();
    public static Map<String, IOBlockedRunnable> socketMap = Collections.synchronizedMap(new HashMap<String, IOBlockedRunnable>());    //套接字Map
    private CompositeDisposable disposableManager = new CompositeDisposable();          //通道管理器
    private Map<String, NodeLayout> nodeLayoutMap = new HashMap<>(); //键是ip#typenumber 样式，zigbee率先进来的话是#typeNumber不带ip样式
    private Map<NodeLayout, CollectLayout> collectLayoutMap = new HashMap<>();
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
    public void onViewClicked() {
      
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

            }
        }
    };

}


