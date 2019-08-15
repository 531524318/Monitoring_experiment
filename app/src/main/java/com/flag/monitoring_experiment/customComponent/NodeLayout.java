package com.flag.monitoring_experiment.customComponent;

//import android.annotation.TargetApi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flag.monitoring_experiment.MainActivity;
import com.flag.monitoring_experiment.R;
import com.flag.monitoring_experiment.tcpUtils.IOBlockedRunnable;
import com.flag.monitoring_experiment.tcpUtils.IOBlockedZigbeeRunnable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.flag.monitoring_experiment.MainActivity.ZIGBEE_IP;

//import java.util.function.Predicate;
//import java.util.function.ToIntFunction;

/**
 * Created by Bmind on 2018/6/19.
 */

public class NodeLayout extends LinearLayout {
    private static final String TAG = "节点";
    private ImageView wifiswitch;
    private ImageView zigbeeswitch;
    private TextView bindsensor;
    private TextView hiddentext;            //隐藏一些本类需要的信息
    private NodeLayout mThis;
    private LinearLayout nodecontrol;

    public String onlyIp = "";              //唯一标识的该节点
    public String sensorType = "";
    private String linkType;
    private String linkIP;
    private String number;                  //编号
    private View mView;
    private Context mContext;

    private boolean deviceStatue = false; //节点状态，开和关

    public String getLinkIP() {
        return linkIP;
    }

    public String getSensorType() {
        return sensorType;
    }

    public String getLinkType() {
        return linkType;
    }

    private IOBlockedRunnable runLight;
    private String now_angle = "000";
    private Timer timerlight;
    private String perial = "";//定时器时间间隔
    private PopupWindow popupWindow = null;

    private List<Button> quanxiangList = new ArrayList<>();   //存储已经学习过的按钮，显示蓝色样式
    private Button quanxiangBtn = null;                             //表示当前点击选择中的按钮

    public NodeLayout(Context context, AttributeSet attrs) {
        super(context,attrs);
        mContext = context;
        mView = LayoutInflater.from(context).inflate(R.layout.node, this);
        mThis = this;
        wifiswitch = (ImageView) findViewById(R.id.wifiswitch);
        zigbeeswitch = (ImageView) findViewById(R.id.zigbeeswitch);
        bindsensor = (TextView) findViewById(R.id.bindsensor);
        hiddentext = (TextView) findViewById(R.id.hiddentext);
        nodecontrol = (LinearLayout) findViewById(R.id.nodecontrol);
        wifiswitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String jsondata = hiddentext.getText().toString();
                    JSONObject jobject = new JSONObject(jsondata);
//                    if(jobject.getString("wifistatue").equals("true")){
//                        //组成命令字节,发送控制命令
//                        if(((LinearLayout)mThis.getParent()).getId() == R.id.containerLine12        //相等表示在控制节点区域下，所以该点击按钮会发送控制命令
//                                || ((LinearLayout)mThis.getParent()).getId() == R.id.containerLine22){
//                            String wifiip = jobject.getString("wifiip");
//                            String type = jobject.getString("type");
//                            switch (type){
//                                case "水泵":type = "wp";break;
//                                case "风扇":type = "af";break;
//                                case "卷帘":type = "rs";;break;
//                                case "植物生长灯":type = "pl";break;
//                                case "加热器":type = "cr";break;
//                                case "加湿器":type = "hr";break;
//                                /**cssf新增应用**/
//                                case "电磁锁":type = "el";break;
//                                case "可调灯":type = "al";break;
//                                case "继电器":type = "re";break;
//                                case "全向红外":type = "or";break;
//                                case "声光报警":type = "sl";break;
//                            }
//                            if(!wifiip.equals("")){
//                                IOBlockedRunnable run = (IOBlockedRunnable) MainUIActivity.socketMap.get(wifiip);
//                                if (run == null)return;
//                                String order = "Hwc"+type+run.getNumber() + (deviceStatue?"03offT":"02onT");
//                                //对控制电机单独处理，目前不需要设置数值，默认使用00 ,作单独处理
//                                if (type.equals("rs")){     //类型是卷帘电机
//                                    juanlianControl();
//                                    return;
//                                }
//                                if (type.equals("al")){     //类型是可调灯
//                                    run.pw.write("Hwcal"+number+"09startctrlT");
//                                    run.pw.flush();
//                                    lightUserInface(run,"Hw");
//                                    return;
//                                }
//                                if (type.equals("or")){     //类型是全向红外，显示界面
//                                    quanxiangUserInface(run,"Hw");
//                                    return;
//                                }
//                                run.pw.write(order);
//                                run.pw.flush();
//                            }
//                        }
//                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
//        zigbeeswitch.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    String jsondata = hiddentext.getText().toString();
//                    JSONObject jobject = new JSONObject(jsondata);
//                    if(jobject.getString("zigbeestatue").equals("true")){
//                        //组成命令字节,发送控制命令
//                        if(((LinearLayout)mThis.getParent()).getId() == R.id.containerLine12        //相等表示在控制节点区域下，所以该点击按钮会发送控制命令
//                                || ((LinearLayout)mThis.getParent()).getId() == R.id.containerLine22){
//                            String zigbeeip = jobject.getString("zigbeeip");
//                            String type = jobject.getString("type");
//                            switch (type){
//                                case "水泵":type = "wp";break;
//                                case "风扇":type = "af";break;
//                                case "卷帘":type = "rs";break;
//                                case "植物生长灯":type = "pl";break;
//                                case "加热器":type = "cr";break;
//                                case "加湿器":type = "hr";break;
//                                /**cssf新增应用**/
//                                case "电磁锁":type = "el";break;
//                                case "可调灯":type = "al";break;
//                                case "继电器":type = "re";break;
//                                case "全向红外":type = "or";break;
//                                case "声光报警":type = "sl";break;
//                            }
//                            if(!zigbeeip.equals("")){
//                                IOBlockedZigbeeRunnable run = (IOBlockedZigbeeRunnable) MainUIActivity.socketMap.get(zigbeeip);
//                                //对控制电机单独处理，目前不需要设置数值，默认使用00 ,作单独处理
//                                if (type.equals("rs")){     //卷帘电机
//                                    juanlianControl();
//                                    return;
//                                }
//                                if (type.equals("al")){     //类型是可调灯
//                                    run.pw.write("Hzcal"+number+"09startctrlT");
//                                    run.pw.flush();
//                                    lightUserInface(run,"Hz");
//                                    return;
//                                }
//                                if (type.equals("or")){     //类型时全向红外，显示界面
//                                    quanxiangUserInface(run,"Hz");
//                                    return;
//                                }
//                                Message msgInfo = new Message();
//                                msgInfo.what = 0x110;
//                                msgInfo.obj = number + sensorType + "控制中...";
//                                handler.sendMessage(msgInfo);
//                                zigbeeswitch.setEnabled(false);
////                                handler.sendEmptyMessageDelayed(0x111,5000);
//                                Log.d(TAG, "发送控制指令: "+"Hzc"+type+number + (deviceStatue?"03offT":"02onT"));
//                                run.pw.write("Hzc"+type+number + (deviceStatue?"03offT":"02onT"));
//                                run.pw.flush();
//                            }
//                        }
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        });

    }
    private String QUANXIANG_STATUS = ""; //STUDY,SEND,CLEAR_ONE,CLEAR_ALL
    private GridLayout container = null;
    //全向红外界面
    private void quanxiangUserInface(IOBlockedRunnable run, String orderhead) {
        runLight = run;
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_quanxiang,null);
        int orignalWidth = 1150,orignalHeight = 950;
        popupWindow = new PopupWindow(view,orignalWidth,orignalHeight,true);
        popupWindow.showAtLocation(mView, Gravity.CENTER,20,20);
        init_quanxiang(view,orderhead);
        container = (GridLayout) view.findViewById(R.id.container_quanxiang);
        int width = orignalWidth / 11;
        int height = (orignalHeight - 250) / 10;
        for(int i = 0;i < 100;i++){                 //添加100个按钮
            Button bn = new Button(mContext);
            bn.setText(i<10?"0"+i:i+"");
            bn.setTextSize(20);

            bn.setOnClickListener(new OnClickListener() {
                @SuppressLint("NewApi")
                @Override
                public void onClick(View v) {
                    if (quanxiangBtn != null){
                        if (quanxiangBtn.getBackground().equals(getResources().getDrawable(R.drawable.btnclicktrue,null))){

                        }else{
                            quanxiangBtn.setBackgroundResource(R.drawable.blue_btn);
                        }
                        if(quanxiangList.contains(quanxiangBtn))
                            quanxiangBtn.setBackgroundResource(R.drawable.button_style);//记忆学习过的按钮
                    }
                    quanxiangBtn = (Button)v;   //quanxiangBtn指向当前的点击的按钮
                    quanxiangBtn.setBackgroundResource(R.drawable.button_link);
                }
            });
            bn.setBackgroundResource(R.drawable.blue_btn);
            GridLayout.Spec rowspec= GridLayout.spec(i/10);
            GridLayout.Spec columnspec= GridLayout.spec(i%10);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowspec,columnspec);
            params.width = width;
            params.leftMargin = 5;
            params.rightMargin = 5;
            params.bottomMargin = 5;
            params.height = height;
            params.setGravity(Gravity.FILL);
            container.addView(bn,params);
        }
    }


    //对全向红外界面进行初始化
    private void init_quanxiang(View view, final String orderhead) {
        ImageView imageView = (ImageView) view.findViewById(R.id.iv_close_quanxiang);
        Button refresh_quanxiang = (Button) view.findViewById(R.id.refresh_quanxiang);
        Button study_quanxiang = (Button) view.findViewById(R.id.study_quanxiang);
        Button send_quanxiang = (Button) view.findViewById(R.id.send_quanxiang);
        Button clear_one_quanxiang = (Button) view.findViewById(R.id.clear_one_quanxiang);
        Button clear_all_quanxiang = (Button) view.findViewById(R.id.clear_all_quanxiang);
        refresh_quanxiang.setOnClickListener(new OnClickListener() {    //刷新
            @Override
            public void onClick(View v) {
                //发送刷新按钮指令，获取返回信息
                if (!QUANXIANG_STATUS.equals(""))return;//一定要等待其他按钮按下3秒之后，状态恢复了，这个按钮才能起作用
//                String btnNumber = quanxiangBtn.getText().toString();
                runLight.pw.write(orderhead + "cor"+number+"07refreshT");
                runLight.pw.flush();
                QUANXIANG_STATUS = "REFRESH";

                ((Button)v).setText("刷新控制中...");
                ((LinearLayout)(v.getParent())).setEnabled(false);
                //延迟3秒不管成功与否都这样处理数据信息
                Message msg = new Message();
                msg.what = 0x112;
                msg.obj = v;
                handler.sendMessageDelayed(msg,orderhead.contains("Hw")?3000:5000);//zigbee间隔五秒可以使用
            }
        });
        study_quanxiang.setOnClickListener(new OnClickListener() {      //学习
            @Override
            public void onClick(View v) {
                //发送学习指令，获取返回信息
                if (!QUANXIANG_STATUS.equals(""))return;
                if (quanxiangBtn != null){
                    String btnNumber = quanxiangBtn.getText().toString();
                    runLight.pw.write(orderhead + "cor" + number + "08learn_" + btnNumber + "T");
                    runLight.pw.flush();
                    QUANXIANG_STATUS = "STUDY";
                    ((Button)v).setText("学习控制中...");
                    ((LinearLayout)(v.getParent())).setEnabled(false);
                    v.setEnabled(false);
                    //延迟3秒不管成功与否都这样处理数据信息
                    Message msg = new Message();
                    msg.what = 0x112;
                    msg.obj = v;
                    handler.sendMessageDelayed(msg,orderhead.contains("Hw")?3000:5000);
                }
            }
        });
        send_quanxiang.setOnClickListener(new OnClickListener() {       //发射
            @Override
            public void onClick(View v) {
                //发送发射指令，获取返回信息
                if (!QUANXIANG_STATUS.equals(""))return;
                if (quanxiangBtn != null){
                    String btnNumber = quanxiangBtn.getText().toString();
                    runLight.pw.write(orderhead + "cor" + number + "07send_" + btnNumber + "T");
                    runLight.pw.flush();
                    QUANXIANG_STATUS = "SEND";
                    ((Button)v).setText("发射控制中...");
                    ((LinearLayout)(v.getParent())).setEnabled(false);
                    v.setEnabled(false);
                    //延迟3秒不管成功与否都这样处理数据信息
                    Message msg = new Message();
                    msg.what = 0x112;
                    msg.obj = v;
                    handler.sendMessageDelayed(msg,orderhead.contains("Hw")?3000:5000);
                }
            }
        });
        clear_one_quanxiang.setOnClickListener(new OnClickListener() {  //清除
            @Override
            public void onClick(View v) {
                //发送清除单个指令，获取返回信息
                if (!QUANXIANG_STATUS.equals(""))return;
                if (quanxiangBtn != null){
                    String btnNumber = quanxiangBtn.getText().toString();
                    runLight.pw.write(orderhead + "cor" + number + "12clear_one_" + btnNumber + "T");
                    runLight.pw.flush();
                    QUANXIANG_STATUS = "CLEAR_ONE";
                    ((Button)v).setText("清除单个中...");
                    ((LinearLayout)(v.getParent())).setEnabled(false);
                    v.setEnabled(false);
                    //延迟3秒不管成功与否都这样处理数据信息
                    Message msg = new Message();
                    msg.what = 0x112;
                    msg.obj = v;
                    handler.sendMessageDelayed(msg,orderhead.contains("Hw")?3000:5000);
                }
            }
        });
        clear_all_quanxiang.setOnClickListener(new OnClickListener() {  //清除所有
            @Override
            public void onClick(View v) {
                //发送清除所有指令，获取返回信息
                if (!QUANXIANG_STATUS.equals(""))return;
                runLight.pw.write(orderhead + "cor" + number + "09clear_allT");
                runLight.pw.flush();
                QUANXIANG_STATUS = "CLEAR_ALL";
                ((Button)v).setText("清除全部中...");
                ((LinearLayout)(v.getParent())).setEnabled(false);
                v.setEnabled(false);
                //延迟3秒不管成功与否都这样处理数据信息
                Message msg = new Message();
                msg.what = 0x112;
                msg.obj = v;
                handler.sendMessageDelayed(msg,orderhead.contains("Hw")?3000:5000);
            }
        });
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                popupWindow = null;
            }
        });

    }

    //可调灯控制
    /**
     * 可调灯界面
     * @param run  任务对象
     * @param orderHeader 命令头
     */
    private void lightUserInface(IOBlockedRunnable run, final String orderHeader) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_light,null);
        popupWindow = new PopupWindow(view,650,450,true);
        popupWindow.showAtLocation(mView, Gravity.CENTER,20,20);
        ImageView imageView = (ImageView) view.findViewById(R.id.iv_close_light);
        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (orderHeader=="Hw"){     //wifi控制才发送，zigbee控制不发送
                    String orderlight = orderHeader +"cal"+number+"08stopctrlT";
                    runLight.pw.write(orderlight);
                    runLight.pw.flush();
                }
                popupWindow.dismiss();
                popupWindow = null;
            }
        });
        runLight = run;
        SeekBar light_control = (SeekBar) view.findViewById(R.id.light_control);
        int parseInt = Integer.parseInt(now_angle);
        light_control.setProgress(parseInt);
        final TextView light_angle = (TextView) view.findViewById(R.id.light_angle);
        light_angle.setText("亮度:"+parseInt+"%");
        EditText control_perial = (EditText) view.findViewById(R.id.control_perial);
        control_perial.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                perial = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        light_control.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                light_angle.setText("亮度:"+progress+"%");
                String angle = "100";
                if((progress+"").length()==1){
                    angle =  "00" + progress;
                }else if((progress+"").length()==2){
                    angle =  "0" + progress;
                }
                now_angle = angle;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (orderHeader.contains("Hw")){
                    TimerTask tasklight = new TimerTask(){
                        @Override
                        public void run() {
                            String orderlight = orderHeader + "cal"+runLight.getNumber()+"03"+now_angle+"T";
                            runLight.pw.write(orderlight);
                            runLight.pw.flush();
                        }
                    };
                    timerlight = new Timer();
                    try{
                        if (!perial.equals("")){
                            timerlight.schedule(tasklight,0, Integer.parseInt(perial));
                        }else{
                            timerlight.schedule(tasklight,0,500);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (orderHeader.contains("Hw")){
                    String orderlight = orderHeader + "cal"+runLight.getNumber()+"06"+now_angle+"endT";
                    runLight.pw.write(orderlight);
                    runLight.pw.flush();
                    if (timerlight!=null){
                        timerlight.cancel();
                        timerlight = null;
                    }
                    seekBar.setEnabled(false);
                    Message msgRelift = new Message();
                    msgRelift.what = 0x117;
                    msgRelift.obj = seekBar;
                    handler.sendMessageDelayed(msgRelift,1000);
                }
            }
        });
        if (!orderHeader.contains("Hw")){   //不包含Hw 表示就是Zigbee 传输的，显示zigbee按钮
            Button sendOrderBtn = (Button) view.findViewById(R.id.send_light_order);
            sendOrderBtn.setVisibility(VISIBLE);
            sendOrderBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    String orderlight = orderHeader + "cal"+number+"06"+now_angle+"endT";
                    runLight.pw.write(orderlight);
                    runLight.pw.flush();
                }
            });
        }
    }
    public JSONObject getHiddenTextJSON(){
        try {
            return new JSONObject(hiddentext.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateHiddenText(String data){          //更新隐藏信息内容
        String[] keyValue = data.split("#");
        if (keyValue.length != 0){
            String jsondata = hiddentext.getText().toString();
            JSONObject jobject = null;
            try {
                //zigbeeip##zigbeestatue#false
                jobject = new JSONObject(jsondata);
                for(int i=0;i < keyValue.length;i++){
                    if (i%2==0){
                        jobject.put(keyValue[i],keyValue[i+1]);
                        if (keyValue[i].equals("wifiip")){
                            if (!keyValue[i+1].equals("")){
                                jobject.put("wifistatue","true");
                                jobject.put("zigbeestatue","false");
                                wifiswitch.setVisibility(VISIBLE);
                                zigbeeswitch.setVisibility(GONE);
                                this.linkType = "wifi";
                                this.linkIP = keyValue[i+1];
                            }else{
                                jobject.put("wifistatue","false");
                                jobject.put("zigbeestatue","true");
                                wifiswitch.setVisibility(GONE);
                                zigbeeswitch.setVisibility(VISIBLE);
                                this.linkType = "zigbee";
                            }
                        }else if(keyValue[i].equals("zigbeeip")){
                            if (!keyValue[i+1].equals("")){
                                jobject.put("zigbeestatue","true");
//                                jobject.put("wifistatue","false");
                                zigbeeswitch.setVisibility(VISIBLE);
                                wifiswitch.setVisibility(GONE);
                                this.linkType = "zigbee";
                                this.linkIP = keyValue[i+1];
                            }else{
                                jobject.put("zigbeestatue","false");
                                jobject.put("wifistatue","true");
                                zigbeeswitch.setVisibility(GONE);
                                wifiswitch.setVisibility(VISIBLE);
                                this.linkType = "wifi";
                            }
                        }
                    }
                }
                sensorType = jobject.getString("type");
                number =  jobject.getString("number");
                hiddentext.setText(jobject.toString());
                if (sensorType.equals("卷帘")){

                }else{
                    bindsensor.setText(number + sensorType);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0x110:
                    String content = msg.obj.toString();
                    bindsensor.setText(content);
                    break;
                case 0x111:
                    zigbeeswitch.setEnabled(true);
                    break;
                case 0x112:     //延迟3秒不管成功与否都这样处理数据信息
                    for (Button btn : quanxiangList){
                        btn.setBackgroundResource(R.drawable.button_style);
                    }
                    if (msg.obj instanceof Button){
                        Button btn = (Button) msg.obj;
                        btn.setText(btn.getText().toString().substring(0,4));
                        btn.setEnabled(true);
                        ((LinearLayout)(btn.getParent())).setEnabled(true);
                    }
                    QUANXIANG_STATUS = "";
                    break;
                case 0x113:     //立刻执行--》清除所有节点界面更新
                    String clearall = msg.obj.toString();
                    Toast.makeText(mContext,clearall, Toast.LENGTH_SHORT).show();
                    if (clearall.contains("成功")){
                        for (Button btn : quanxiangList){
                            btn.setBackgroundResource(R.drawable.blue_btn);
                        }
                        quanxiangList.removeAll(quanxiangList);
                    }
                    QUANXIANG_STATUS = "";
                    break;
                case 0x114:     //立刻执行-》清除单个节点返回信息处理,更改显示界面
                    String sclear = msg.obj.toString();
                    Toast.makeText(mContext,msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    if (sclear.contains("成功"))
                        quanxiangBtn.setBackgroundResource(R.drawable.blue_btn);
                    QUANXIANG_STATUS = "";
                    break;
                case 0x115:     //立刻执行-》学习命令返回信息处理
                    String sstudy = msg.obj.toString();
                    Toast.makeText(mContext,msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    if(sstudy.contains("成功"))
                        quanxiangBtn.setBackgroundResource(R.drawable.btnclicktrue);
                    QUANXIANG_STATUS = "";
                    break;
                case 0x116:     //立刻执行-->发射命令界面更新，刷新
                    Toast.makeText(mContext,msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    QUANXIANG_STATUS = "";
                    break;
                case 0x117:     //延迟一秒，可调灯再次生效
                    SeekBar bar = (SeekBar) msg.obj;
                    bar.setEnabled(true);
                    break;
            }
        }
    };
    //处理返回信息，改变显示状态
    public void setStatueControl(String restr){
        //解析控制节点的状态信息，例Hwdwp0102onT
        String numberData = restr.substring(5,7);
        if (restr.startsWith("H")&&restr.charAt(2)=='d'&&restr.endsWith("T"))//检查是否符合条件
        {
            if (sensorType.equals("可调灯")){
                if (restr.contains("breath"))           //这是普通的自动上传状态信息，不用管它，跳过就行
                    return;
                int angle = 0;
                try{
                    if(restr.charAt(1)=='w'){
                        now_angle = restr.substring(9,9 + Integer.parseInt(restr.substring(7,9)));
                        angle = Integer.parseInt(now_angle);
                    }else if (restr.charAt(1)=='z'){
                        //是状态帧就改变angle值
                        now_angle = restr.substring(9,9 + Integer.parseInt(restr.substring(7,9)));
                        angle = Integer.parseInt(now_angle);
                    }
                }catch (Exception e){
                }
                if (popupWindow != null){
                    SeekBar light_control = (SeekBar) popupWindow.getContentView().findViewById(R.id.light_control);
                    light_control.setProgress(Integer.valueOf(angle));
                }
                return;
            }
            if(sensorType.equals("全向红外")){
                //处理返回信息
                if (restr.contains("breath"))           //这是普通的自动上传状态信息，不用管它，跳过就行
                    return;
                switch (QUANXIANG_STATUS){
                    case "REFRESH":
                        Message msgRefresh = new Message();
                        msgRefresh.what = 0x116;
                        msgRefresh.obj = "获取刷新节点状态失败";
                        if(restr.length()==113){
                            for(Button btn:quanxiangList)       //清除所有已记忆按钮的样式，还原未记忆按钮样式
                                btn.setBackgroundResource(R.drawable.blue_btn);
                            quanxiangList.removeAll(quanxiangList);
                            char[] hundred = restr.substring(12,restr.length()).toCharArray();
                            for (int i=0;i<hundred.length;i++)//记录下等于'1'的字符的序号，该序号表示按钮是已经学习过命令的
                                if (hundred[i] == '1')
                                    quanxiangList.add((Button) container.getChildAt(i));//加入列表，等待处理
                            msgRefresh.obj = "获取刷新节点状态成功";
                        }
                        handler.sendMessage(msgRefresh);
                        break;
                    case "STUDY":
                        //Hwdor0108learn_okT
                        Message msgStu = new Message();
                        msgStu.what = 0x115;
                        msgStu.obj = "学习命令发送失败";
                        if (restr.length() == 18){
                            quanxiangBtn.setBackgroundResource(R.drawable.btnclicktrue);
//                            if(quanxiangList.add(quanxiangBtn)){
                                msgStu.obj = "学习命令发送成功";
                        }
//                            }
                        handler.sendMessage(msgStu);
                        break;
                    case "SEND":
                        //Hwdor0107send_okT
                        Message msgSend = new Message();
                        msgSend.what = 0x116;
                        msgSend.obj = "发射命令失败";
                        if (restr.length() == 17)
                            msgSend.obj = "发射命令成功";
                        handler.sendMessage(msgSend);
                        break;
                    case "CLEAR_ONE":
                        //Hwdor0112clear_one_okT
                        Message msgClear = new Message();
                        msgClear.what = 0x114;
                        msgClear.obj = "清除单个节点失败";
                        if (restr.length()==22)
                            //移除等于节点的号,版本太低Lambda无法使用
//                            if(quanxiangList.removeIf(obj->((Button)obj).equals(quanxiangBtn)))
                        {
                            Iterator<Button> iterator = quanxiangList.iterator();
                            while(iterator.hasNext()){
                                if (((Button)iterator.next()).getText().toString().equals(quanxiangBtn
                                .getText().toString())){
                                    iterator.remove();
                                    msgClear.obj = "清除单个节点成功";
                                    break;
                                }
                            }
                        }
                        handler.sendMessage(msgClear);
                        break;
                    case "CLEAR_ALL":
                        //Hwdor0111clear_all_okT
                        Message msgClearAll = new Message();
                        msgClearAll.what = 0x113;
                        msgClearAll.obj = "清除所有节点失败";
                        if (restr.length() == 22)
                            msgClearAll.obj = "清除所有节点成功";
                        handler.sendMessage(msgClearAll);
                        break;
                }
                return;
            }
            if (restr.substring(9, 9 + Integer.parseInt(restr.substring(7, 9))).equals("on")) {
                bindsensor.setText(numberData + sensorType + ":开");
                deviceStatue = true;
            } else {
                bindsensor.setText(numberData + sensorType + ":关");
                deviceStatue = false;
            }
            zigbeeswitch.setEnabled(true);
        }
    }


    public int hideWifi(){
        String jsondata = hiddentext.getText().toString();

        IOBlockedZigbeeRunnable run = (IOBlockedZigbeeRunnable) MainActivity.socketMap.get("/"+ZIGBEE_IP);
        if (run == null){
            wifiswitch.setVisibility(GONE);
            zigbeeswitch.setVisibility(VISIBLE);
            updateHiddenText("wifiip##wifistatue#false");
            return 2;
        }else{
            for (IOBlockedZigbeeRunnable.ZigbeeDevice temp: run.getDeviceList()){
                if (temp.getNumber().equals(number)){
                    Log.d(TAG, "hideWifi: 找到了");
                    return 1;
                }
            }
            return 2;
        }
    }

    public void hideWifiTrue(){
        if (wifiswitch!=null)
        wifiswitch.setVisibility(GONE);
//        updateHiddenText("wifiip##wifistatue#false");
    }

    public int hideZigbee(){         //返回值表示，1 单单移除zigbee， 2 wifi 控制此时也不在线，一块移除
        String jsondata = hiddentext.getText().toString();
        JSONObject jobject = null;
        String wifiip = "";
        try {
            jobject = new JSONObject(jsondata);
            wifiip = jobject.getString("wifiip");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        IOBlockedRunnable run = (IOBlockedRunnable) MainActivity.socketMap.get(wifiip);
        if (run == null){
            wifiswitch.setVisibility(VISIBLE);
            zigbeeswitch.setVisibility(GONE);
            updateHiddenText("zigbeeip##zigbeestatue#false");
            return 2;
        }else{
            return 1;
        }
    }
    public void hideZigbeeTrue(){
        if (zigbeeswitch != null){
            zigbeeswitch.setVisibility(View.GONE);
        }
        updateHiddenText("zigbeeip##zigbeestatue#false");
    }


    public void setImageBackgroundCanClick(){
        wifiswitch.setImageResource(R.drawable.wifipress);
        zigbeeswitch.setImageResource(R.drawable.zigbeepress);
    }

    public void setDefaultType(){
        if (sensorType.equals("卷帘")) {
            bindsensor.setText(number + "卷帘控制反转");
        }
    }
}
