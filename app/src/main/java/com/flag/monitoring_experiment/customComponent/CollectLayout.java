package com.flag.monitoring_experiment.customComponent;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.glhz.wisdomagriculture.MainUIActivity;
import com.glhz.wisdomagriculture.R;
import com.glhz.wisdomagriculture.tcpUtils.IOBlockedRunnable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by Bmind on 2018/6/19.
 */

public class CollectLayout extends LinearLayout {
    public String TAG = "CollectLayout";
    private LinearLayout collectContainer;
    private ImageView imageType;
    private TextView showLink;
    private boolean isCollectSensor = false;        //是否是采集传感器
    private String bindipAddress;                   //当前该传感器连接的IP地址
    public String sensorType;
    private String number;
    private int tableWidth;     //记录屏幕宽和高
    private int tableHeight;
    private Activity mActivity;
    private TextView temshowValue;
    private TextView humshowValue;

    private TextView pm25,pm25lz,pm10,pm10lz;

    PopupWindow popup;
    IOBlockedRunnable run;

    private ImageView iv_hasornot = null;


    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public String getNumber() {
        return number;
    }

    public CollectLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.collect,this);
        showLink = (TextView) findViewById(R.id.showLink);
        imageType = (ImageView) findViewById(R.id.imageType);
        collectContainer = (LinearLayout) findViewById(R.id.collectContainer);


        collectContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCollectSensor){               //是采集传感器而不是控制设备，就显示出数据状态折线图
                    if(!bindipAddress.equals("")){
                        Log.d(TAG, "onClick: "+bindipAddress);
                        String nowIP = bindipAddress;
                        if (!bindipAddress.startsWith("/"))
                            nowIP = "/"+bindipAddress;
                        run = (IOBlockedRunnable) MainUIActivity.socketMap.get(nowIP);
                        run.informMainIpAndType(bindipAddress+"#"+sensorType+number);         //告知主线程ip地址
                        switch (sensorType) {
                            case "温湿度":
                                LinearLayout temhumLayout = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.dialog_temhum,null);
                                initChartTem(temhumLayout);                     //初始化折线图
                                popup = new PopupWindow(temhumLayout, tableWidth / 6 * 4, tableHeight / 6 * 4);
                                popup.setFocusable(true);           //点击其他空处没有作用
                                popup.showAtLocation(mActivity.findViewById(R.id.activity_main_ui), Gravity.CENTER,20,20);
                                temhumLayout.findViewById(R.id.iv_close_temhum).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        popup.dismiss();
                                        popup = null;
                                        position = 0;position_hum = 0;
//                                        run.notifyRunSendToMain(false);
                                        run.informMainIpAndType("");        //把空字符串传递过去,前台把nowCollect对象置空
                                    }
                                });

                                break;
                            case "光照度":
                                LinearLayout illiLayout = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.dialog_guangzhao,null);
                                initChartGuang(illiLayout);                     //初始化折线图
                                popup = new PopupWindow(illiLayout, tableWidth / 6 * 4, tableHeight / 6 * 4);
                                popup.setFocusable(true);           //点击其他空处没有作用
                                popup.showAtLocation(mActivity.findViewById(R.id.activity_main_ui), Gravity.CENTER,20,20);
                                illiLayout.findViewById(R.id.iv_close_guangzhao).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        popup.dismiss();
                                        popup = null;
                                        position = 0;
//                                        run.notifyRunSendToMain(false);
                                        run.informMainIpAndType("");
                                    }
                                });
                                break;
                            case "二氧化碳":
                                LinearLayout co2Layout = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.dialog_co2,null);
                                initChartCo2(co2Layout);                     //初始化折线图
                                popup = new PopupWindow(co2Layout, tableWidth / 6 * 4, tableHeight / 6 * 4);
                                popup.setFocusable(true);           //点击其他空处没有作用
                                popup.showAtLocation(mActivity.findViewById(R.id.activity_main_ui), Gravity.CENTER,20,20);
                                co2Layout.findViewById(R.id.iv_close_co2).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        popup.dismiss();
                                        popup = null;
                                        position = 0;
//                                        run.notifyRunSendToMain(false);
                                        run.informMainIpAndType("");
                                    }
                                });
                                break;
                            case "氧气":
                                LinearLayout o2Layout = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.dialog_o2,null);
                                initChartO2(o2Layout);                     //初始化折线图
                                popup = new PopupWindow(o2Layout, tableWidth / 6 * 4, tableHeight / 6 * 4);
                                popup.setFocusable(true);           //点击其他空处没有作用
                                popup.showAtLocation(mActivity.findViewById(R.id.activity_main_ui), Gravity.CENTER,20,20);
                                o2Layout.findViewById(R.id.iv_close_o2).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        popup.dismiss();
                                        popup = null;
                                        position = 0;
//                                        run.notifyRunSendToMain(false);
                                        run.informMainIpAndType("");
                                    }
                                });
                                break;
                            case "大气压":
                                LinearLayout paLayout = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.dialog_pa,null);
                                initChartPa(paLayout);                     //初始化折线图
                                popup = new PopupWindow(paLayout, tableWidth / 6 * 4, tableHeight / 6 * 4);
                                popup.setFocusable(true);           //点击其他空处没有作用
                                popup.showAtLocation(mActivity.findViewById(R.id.activity_main_ui), Gravity.CENTER,20,20);
                                paLayout.findViewById(R.id.iv_close_pa).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        popup.dismiss();
                                        popup = null;
                                        position = 0;
//                                        run.notifyRunSendToMain(false);
                                        run.informMainIpAndType("");
                                    }
                                });
                                break;
                            case "粉尘":
                                LinearLayout dustLayout = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.dialog_dust,null);
                                initChartDust(dustLayout);                     //初始化折线图
                                popup = new PopupWindow(dustLayout, tableWidth / 6 * 3, tableHeight / 6 * 3);
                                popup.setFocusable(true);           //点击其他空处没有作用
                                popup.showAtLocation(mActivity.findViewById(R.id.activity_main_ui), Gravity.CENTER,20,20);
                                dustLayout.findViewById(R.id.iv_close_dust).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        popup.dismiss();
                                        popup = null;
                                        position = 0;
//                                        run.notifyRunSendToMain(false);
                                        run.informMainIpAndType("");
                                    }
                                });
                                break;
                            case "人体红外":
                            case "红外对射":
                            case "可燃气体":
                            case "火焰气体":
                                LinearLayout hasOrnotLayout = (LinearLayout) mActivity.getLayoutInflater().inflate(R.layout.dialog_hasornot,null);
                                popup = new PopupWindow(hasOrnotLayout, tableWidth / 6 * 3, tableHeight / 6 * 3);
                                popup.setFocusable(true);           //点击其他空处没有作用
                                popup.showAtLocation(mActivity.findViewById(R.id.activity_main_ui), Gravity.CENTER,20,20);
                                ((TextView)(hasOrnotLayout.findViewById(R.id.hasornot_Tittle))).setText(sensorType + "传感器");
                                iv_hasornot = (ImageView)hasOrnotLayout.findViewById(R.id.iv_hasornot);
                                hasOrnotLayout.findViewById(R.id.iv_close_hasornot).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        popup.dismiss();
                                        popup = null;
                                        run.informMainIpAndType("");
                                        iv_hasornot = null;
                                    }
                                });
                                break;
                        }
                    }
                }
            }
        });
    }

    public void setShowInit(String type, String wifiOrZigbee, String ipAddress, String number){       //设置第一次连接UI显示状态
        int imageId = 0;
        this.bindipAddress = ipAddress;         //设置IP方便索引到Runnable对象
        this.sensorType = type;
        this.number = number;
        switch (type){
            case "温湿度":imageId = R.drawable.temhum;isCollectSensor = true;break;
            case "光照度":imageId = R.drawable.guangmin;isCollectSensor = true;break;
            case "二氧化碳":imageId = R.drawable.co2;isCollectSensor = true;break;
            case "氧气":imageId = R.drawable.o2;isCollectSensor = true;break;
            case "大气压":imageId = R.drawable.daqiya;isCollectSensor = true;break;
            case "粉尘":imageId = R.drawable.dust;isCollectSensor = true;break;
            case "风扇":imageId = R.drawable.fengshan;break;
            case "水泵":imageId = R.drawable.shuibeng;break;
            case "卷帘":imageId = R.drawable.dianji;break;
            case "植物生长灯":imageId = R.drawable.deng;break;
            case "加热器":imageId = R.drawable.jiare;break;
            case "加湿器":imageId = R.drawable.jiashi;break;

            /**cssf新增应用**/
            case "电磁锁":imageId = R.drawable.lock;break;
            case "可调灯":imageId = R.drawable.light;break;
            case "继电器":imageId = R.drawable.switchp;break;
            case "全向红外":imageId = R.drawable.quanxiang;break;
            case "声光报警":imageId = R.drawable.sounglight;break;

            case "人体红外":imageId = R.drawable.bodyinfrared;isCollectSensor = true;break;
            case "红外对射":imageId = R.drawable.infraredfence;isCollectSensor = true;break;
            case "可燃气体":imageId = R.drawable.gas;isCollectSensor = true;break;
            case "火焰气体":imageId = R.drawable.flame;isCollectSensor = true;break;
        }
        if (imageId != 0){
            imageType.setBackgroundResource(imageId);
        }
        String orignal = number+"当前连接"+wifiOrZigbee;
        SpannableStringBuilder style=new SpannableStringBuilder(orignal);
        style.setSpan(new ForegroundColorSpan(Color.YELLOW),6,orignal.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        showLink.setText(style);
    }

    public void changeStatueLink(String type, String ipAddress){   //改变连接显示状态
        if (!type.equals("")){
            String orignal = number+"当前连接"+type;
            this.bindipAddress = ipAddress;                             //更改当前连接的ip 信息
            SpannableStringBuilder style=new SpannableStringBuilder(orignal);
            style.setSpan(new ForegroundColorSpan(Color.YELLOW),6,orignal.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            showLink.setText(style);
            closeUIPopWindow();
        }
    }

    public void changeStatueLinkNoPOP(String type, String ipAddress){   //改变连接显示状态
        if (!type.equals("")){
            String orignal = number+"当前连接"+type;
            this.bindipAddress = ipAddress;                             //更改当前连接的ip 信息
            SpannableStringBuilder style=new SpannableStringBuilder(orignal);
            style.setSpan(new ForegroundColorSpan(Color.YELLOW),6,orignal.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            showLink.setText(style);
        }
    }

    public void setConfig(Activity ac, int tableWidth, int tableHeight){
        this.mActivity = ac;
        this.tableWidth = tableWidth;
        this.tableHeight = tableHeight;
    }

    private LineChartView charTem;
    private Axis axisY;                     //Y坐标
    private Axis axisX;                     //X坐标
    private LinkedList mPointValue;         //点值链表
    private List<Line> mLine;               //线条列表
    private float position = 0;             //横坐标位置

    private LineChartView charHum;
    private Axis axisY_hum;                     //Y坐标
    private Axis axisX_hum;                     //X坐标
    private LinkedList mPointValue_hum;         //点值链表
    private List<Line> mLine_hum;               //线条列表
    private float position_hum = 0;             //横坐标位置0


    private void initChartTem(LinearLayout temhumLayout) {
        charTem = (LineChartView) temhumLayout.findViewById(R.id.line_char_tem);
        charHum = (LineChartView) temhumLayout.findViewById(R.id.line_char_hum);
        temshowValue = (TextView) temhumLayout.findViewById(R.id.temshowvalue);
        humshowValue = (TextView) temhumLayout.findViewById(R.id.humshowvalue);
        /***温度初始化***/
        charTem.setInteractive(true);
        charTem.setZoomType(ZoomType.HORIZONTAL);  //缩放类型，水平
        charTem.setMaxZoom((float) 3);//缩放比例
        charTem.setVisibility(View.VISIBLE);
        charTem.setZoomEnabled(true);

        axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(false);  //X轴下面坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.parseColor("#fcfffc"));
        axisX.setLineColor(Color.parseColor("#fcfffc"));
        axisX.setTextSize(11);              //设置字体大小
        axisX.setMaxLabelChars(7);
        axisX.setName("温度变化曲线");

        axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setLineColor(Color.parseColor("#fcfffc"));
        axisY.setTextColor(Color.parseColor("#fcfffc"));
        axisY.setName("摄氏度℃");

        List list = new LinkedList();
        LineChartData data = new LineChartData(list);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        mPointValue = new LinkedList();             //实例化 值链表
        mLine = new ArrayList<>();                  //实例化
        pointUpdate((float)0);
        /***湿度初始化***/
        charHum.setInteractive(true);
        charHum.setZoomType(ZoomType.HORIZONTAL);  //缩放类型，水平
        charHum.setMaxZoom((float) 3);//缩放比例
        charHum.setVisibility(View.VISIBLE);
        charHum.setZoomEnabled(true);

        axisX_hum = new Axis(); //X轴
        axisX_hum.setHasTiltedLabels(false);  //X轴下面坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX_hum.setTextColor(Color.parseColor("#fcfffc"));
        axisX_hum.setLineColor(Color.parseColor("#fcfffc"));
        axisX_hum.setTextSize(11);              //设置字体大小
        axisX_hum.setMaxLabelChars(7);
        axisX_hum.setName("湿度变化曲线");

        axisY_hum = new Axis();
        axisY_hum.setAutoGenerated(true);
        axisY_hum.setLineColor(Color.parseColor("#fcfffc"));
        axisY_hum.setTextColor(Color.parseColor("#fcfffc"));
        axisY_hum.setName("RH");

        List list_hum = new LinkedList();
        LineChartData data_hum = new LineChartData(list_hum);
        data_hum.setAxisXBottom(axisX_hum);
        data_hum.setAxisYLeft(axisY_hum);
        charHum.setLineChartData(data_hum);

        mPointValue_hum = new LinkedList();             //实例化 值链表
        mLine_hum = new ArrayList<>();                  //实例化
        pointUpdate_hum((float)0);
    }
    private void initChartGuang(LinearLayout temhumLayout) {
        charTem = (LineChartView) temhumLayout.findViewById(R.id.line_char_guang);

        temshowValue = (TextView) temhumLayout.findViewById(R.id.guangzhaoshowvalue);

        List list = new LinkedList();
        LineChartData data = new LineChartData(list);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        mPointValue = new LinkedList();             //实例化 值链表
        mLine = new ArrayList<>();                  //实例化
        pointUpdate_guang((float)0);
        /***光照度初始化***/
        charTem.setInteractive(true);
        charTem.setZoomType(ZoomType.HORIZONTAL);  //缩放类型，水平
        charTem.setMaxZoom((float) 3);//缩放比例
        charTem.setVisibility(View.VISIBLE);
        charTem.setZoomEnabled(true);

        axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(false);  //X轴下面坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.parseColor("#fcfffc"));
        axisX.setLineColor(Color.parseColor("#fcfffc"));
        axisX.setTextSize(11);              //设置字体大小
        axisX.setMaxLabelChars(7);
        axisX.setName("光照度变化曲线");

        axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setLineColor(Color.parseColor("#fcfffc"));
        axisY.setTextColor(Color.parseColor("#fcfffc"));
        axisY.setName("lx");

        List list_hum = new LinkedList();
        LineChartData data_hum = new LineChartData(list_hum);
        data_hum.setAxisXBottom(axisX);
        data_hum.setAxisYLeft(axisY);
        charTem.setLineChartData(data_hum);

        pointUpdate_guang((float)0);
    }
    private void initChartCo2(LinearLayout temhumLayout) {
        charTem = (LineChartView) temhumLayout.findViewById(R.id.line_char_co2);

        temshowValue = (TextView) temhumLayout.findViewById(R.id.co2showvalue);
        /***二氧化碳初始化***/
        charTem.setInteractive(true);
        charTem.setZoomType(ZoomType.HORIZONTAL);  //缩放类型，水平
        charTem.setMaxZoom((float) 3);//缩放比例
        charTem.setVisibility(View.VISIBLE);
        charTem.setZoomEnabled(true);

        axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(false);  //X轴下面坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.parseColor("#fcfffc"));
        axisX.setLineColor(Color.parseColor("#fcfffc"));
        axisX.setTextSize(11);              //设置字体大小
        axisX.setMaxLabelChars(7);
        axisX.setName("二氧化碳变化曲线");

        axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setLineColor(Color.parseColor("#fcfffc"));
        axisY.setTextColor(Color.parseColor("#fcfffc"));
        axisY.setName("ppm");

        List list = new LinkedList();
        LineChartData data = new LineChartData(list);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        mPointValue = new LinkedList();             //实例化 值链表
        mLine = new ArrayList<>();                  //实例化
        pointUpdate_co2((float)0);

        List list_hum = new LinkedList();
        LineChartData data_hum = new LineChartData(list_hum);
        data_hum.setAxisXBottom(axisX);
        data_hum.setAxisYLeft(axisY);
        charTem.setLineChartData(data_hum);

        pointUpdate_co2((float)0);
    }
    private void initChartO2(LinearLayout temhumLayout) {
        charTem = (LineChartView) temhumLayout.findViewById(R.id.line_char_o2);

        temshowValue = (TextView) temhumLayout.findViewById(R.id.o2showvalue);
        /***氧气初始化***/
        charTem.setInteractive(true);
        charTem.setZoomType(ZoomType.HORIZONTAL);  //缩放类型，水平
        charTem.setMaxZoom((float) 3);//缩放比例
        charTem.setVisibility(View.VISIBLE);
        charTem.setZoomEnabled(true);

        axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(false);  //X轴下面坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.parseColor("#fcfffc"));
        axisX.setLineColor(Color.parseColor("#fcfffc"));
        axisX.setTextSize(11);              //设置字体大小
        axisX.setMaxLabelChars(7);
        axisX.setName("氧气变化曲线");

        axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setLineColor(Color.parseColor("#fcfffc"));
        axisY.setTextColor(Color.parseColor("#fcfffc"));
        axisY.setName("ppm");

        List list = new LinkedList();
        LineChartData data = new LineChartData(list);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        mPointValue = new LinkedList();             //实例化 值链表
        mLine = new ArrayList<>();                  //实例化
        pointUpdate_o2((float)0);

        List list_hum = new LinkedList();
        LineChartData data_hum = new LineChartData(list_hum);
        data_hum.setAxisXBottom(axisX);
        data_hum.setAxisYLeft(axisY);
        charTem.setLineChartData(data_hum);

        pointUpdate_o2((float)0);
    }
    private void initChartPa(LinearLayout temhumLayout) {
        charTem = (LineChartView) temhumLayout.findViewById(R.id.line_char_pa);

        temshowValue = (TextView) temhumLayout.findViewById(R.id.pashowvalue);
        /**大气压初始化***/
        charTem.setInteractive(true);
        charTem.setZoomType(ZoomType.HORIZONTAL);  //缩放类型，水平
        charTem.setMaxZoom((float) 3);//缩放比例
        charTem.setVisibility(View.VISIBLE);
        charTem.setZoomEnabled(true);

        axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(false);  //X轴下面坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.parseColor("#fcfffc"));
        axisX.setLineColor(Color.parseColor("#fcfffc"));
        axisX.setTextSize(11);              //设置字体大小
        axisX.setMaxLabelChars(7);
        axisX.setName("大气压变化曲线");

        axisY = new Axis();
        axisY.setAutoGenerated(true);
        axisY.setLineColor(Color.parseColor("#fcfffc"));
        axisY.setTextColor(Color.parseColor("#fcfffc"));
        axisY.setName("Pa");

        List list = new LinkedList();
        LineChartData data = new LineChartData(list);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        mPointValue = new LinkedList();             //实例化 值链表
        mLine = new ArrayList<>();                  //实例化
        pointUpdate_pa((float)0);

        List list_hum = new LinkedList();
        LineChartData data_hum = new LineChartData(list_hum);
        data_hum.setAxisXBottom(axisX);
        data_hum.setAxisYLeft(axisY);
        charTem.setLineChartData(data_hum);

        pointUpdate_pa((float)0);
    }
    private void initChartDust(LinearLayout temhumLayout) {
        pm25 = (TextView) temhumLayout.findViewById(R.id.pm25);
        pm25lz = (TextView) temhumLayout.findViewById(R.id.pm25lz);
        pm10 = (TextView) temhumLayout.findViewById(R.id.pm10);
        pm10lz = (TextView) temhumLayout.findViewById(R.id.pm10lz);

    }

    //更新折线图
    private void pointUpdate(Float value){
        float x = 0;
        PointValue p = new PointValue(position,value);
        position++;
        mPointValue.add(p);
        x = p.getX();
        Line line = new Line(mPointValue).setColor(Color.parseColor("#f4a00d"));//设置线条颜色
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.SQUARE）
        line.setCubic(true);//曲线是否平滑
        line.setStrokeWidth(1);//线条的粗细，默认是1
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLines(true);//是否用直线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示
        line.setPointColor(Color.parseColor("#f4a00d"));    //设置point点的颜色
        line.setPointRadius(3);         //设置圆点半径长度

        mLine.add(line);
        LineChartData data = new LineChartData(mLine);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        Viewport port = new Viewport(charTem.getMaximumViewport());
        port.top = 40;              //设置top和bottom，纵坐标的最大值
        port.bottom = 0;
        if(x>8){                            //横向显示7个数据
            port.top = 40;              //无需设置top和bottom，让他默认就行
            port.bottom = 0;
            port.left = x-8;
            port.right = x;
        }
        charTem.setMaximumViewport(port);
        charTem.setCurrentViewport(port);
    }
    private void pointUpdate_hum(Float value){
        float x = 0;
        PointValue p = new PointValue(position_hum,value);
        position_hum++;
        mPointValue_hum.add(p);
        x = p.getX();
        Line line = new Line(mPointValue_hum).setColor(Color.parseColor("#f4a00d"));//设置线条颜色
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.SQUARE）
        line.setCubic(true);//曲线是否平滑
        line.setStrokeWidth(1);//线条的粗细，默认是1
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLines(true);//是否用直线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示
        line.setPointColor(Color.parseColor("#f4a00d"));    //设置point点的颜色
        line.setPointRadius(3);         //设置圆点半径长度

        mLine_hum.add(line);
        LineChartData data = new LineChartData(mLine_hum);
        data.setAxisXBottom(axisX_hum);
        data.setAxisYLeft(axisY_hum);
        charHum.setLineChartData(data);

        Viewport port = new Viewport(charHum.getMaximumViewport());
        port.top = 100;              //设置top和bottom，纵坐标的最大值
        port.bottom = 0;
        if(x>8){                            //横向显示7个数据
            port.top = 100;              //无需设置top和bottom，让他默认就行
            port.bottom = 0;
            port.left = x-8;
            port.right = x;
        }
        charHum.setMaximumViewport(port);
        charHum.setCurrentViewport(port);
    }
    private void pointUpdate_guang(Float value){
        float x = 0;
        PointValue p = new PointValue(position,value);
        position++;
        mPointValue.add(p);
        x = p.getX();
        Line line = new Line(mPointValue).setColor(Color.parseColor("#f4a00d"));//设置线条颜色
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.SQUARE）
        line.setCubic(true);//曲线是否平滑
        line.setStrokeWidth(1);//线条的粗细，默认是1
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLines(true);//是否用直线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示
        line.setPointColor(Color.parseColor("#f4a00d"));    //设置point点的颜色
        line.setPointRadius(3);         //设置圆点半径长度

        mLine.add(line);
        LineChartData data = new LineChartData(mLine);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        Viewport port = new Viewport(charTem.getMaximumViewport());
        if (rangeInDefined(value,0,500))
            port.top = 500;
        else if (rangeInDefined(value,0,1000))
            port.top = 1000;
        else if (rangeInDefined(value,1000,10000))
            port.top = 10000;
        else if (rangeInDefined(value,10000,30000))
            port.top = 30000;
        else if (rangeInDefined(value,30000,60000))
            port.top = 60000;
        else if (rangeInDefined(value,60000,65525))
            port.top = 65525;
        port.bottom = 0;
        if(x>8){                            //横向显示7个数据
//            port.top = 65535;              //无需设置top和bottom，让他默认就行
//            port.bottom = 0;
            port.left = x-8;
            port.right = x;
        }
        charTem.setMaximumViewport(port);
        charTem.setCurrentViewport(port);
    }
    private void pointUpdate_co2(Float value){
        float x = 0;
        PointValue p = new PointValue(position,value);
        position++;
        mPointValue.add(p);
        x = p.getX();
        Line line = new Line(mPointValue).setColor(Color.parseColor("#f4a00d"));//设置线条颜色
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.SQUARE）
        line.setCubic(true);//曲线是否平滑
        line.setStrokeWidth(1);//线条的粗细，默认是1
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLines(true);//是否用直线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示
        line.setPointColor(Color.parseColor("#f4a00d"));    //设置point点的颜色
        line.setPointRadius(3);         //设置圆点半径长度

        mLine.add(line);
        LineChartData data = new LineChartData(mLine);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        Viewport port = new Viewport(charTem.getMaximumViewport());
        if (rangeInDefined(value,0,500))
            port.top = 500;
        else if (rangeInDefined(value,0,1000))
            port.top = 1000;
        else if (rangeInDefined(value,1000,10000))
            port.top = 10000;
        else if (rangeInDefined(value,10000,30000))
            port.top = 30000;
        else if (rangeInDefined(value,30000,60000))
            port.top = 60000;
        else if (rangeInDefined(value,60000,65525))
            port.top = 65525;
        port.bottom = 0;
        if(x>8){                            //横向显示7个数据
//            port.top = 65535;              //无需设置top和bottom，让他默认就行
//            port.bottom = 0;
            port.left = x-8;
            port.right = x;
        }
        charTem.setMaximumViewport(port);
        charTem.setCurrentViewport(port);
    }
    private void pointUpdate_o2(Float value){
        float x = 0;
        PointValue p = new PointValue(position,value);
        position++;
        mPointValue.add(p);
        x = p.getX();
        Line line = new Line(mPointValue).setColor(Color.parseColor("#f4a00d"));//设置线条颜色
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.SQUARE）
        line.setCubic(true);//曲线是否平滑
        line.setStrokeWidth(1);//线条的粗细，默认是1
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLines(true);//是否用直线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示
        line.setPointColor(Color.parseColor("#f4a00d"));    //设置point点的颜色
        line.setPointRadius(3);         //设置圆点半径长度

        mLine.add(line);
        LineChartData data = new LineChartData(mLine);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        Viewport port = new Viewport(charTem.getMaximumViewport());
        if (rangeInDefined(value,0,100))
            port.top = 100;
        else if (rangeInDefined(value,0,500))
            port.top = 500;
        else if (rangeInDefined(value,0,1000))
            port.top = 1000;
        else if (rangeInDefined(value,1000,10000))
            port.top = 10000;
        else if (rangeInDefined(value,10000,30000))
            port.top = 30000;
        else if (rangeInDefined(value,30000,60000))
            port.top = 60000;
        else if (rangeInDefined(value,60000,65525))
            port.top = 65525;
        port.bottom = 0;
        if(x>8){                            //横向显示7个数据
            port.left = x-8;
            port.right = x;
        }
        charTem.setMaximumViewport(port);
        charTem.setCurrentViewport(port);
    }
    private void pointUpdate_pa(Float value){
        float x = 0;
        PointValue p = new PointValue(position,value);
        position++;
        mPointValue.add(p);
        x = p.getX();
        Line line = new Line(mPointValue).setColor(Color.parseColor("#f4a00d"));//设置线条颜色
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.SQUARE）
        line.setCubic(true);//曲线是否平滑
        line.setStrokeWidth(1);//线条的粗细，默认是1
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLines(true);//是否用直线显示。如果为false 则没有曲线只有点显示
        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示
        line.setPointColor(Color.parseColor("#f4a00d"));    //设置point点的颜色
        line.setPointRadius(3);         //设置圆点半径长度

        mLine.add(line);
        LineChartData data = new LineChartData(mLine);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        charTem.setLineChartData(data);

        Viewport port = new Viewport(charTem.getMaximumViewport());
        if (rangeInDefined(value,0,500))
            port.top = 500;
        else if (rangeInDefined(value,0,1000))
            port.top = 1000;
        else if (rangeInDefined(value,1000,10000))
            port.top = 10000;
        else if (rangeInDefined(value,10000,30000))
            port.top = 30000;
        else if (rangeInDefined(value,30000,60000))
            port.top = 60000;
        else if (rangeInDefined(value,60000,65525))
            port.top = 65525;
        port.bottom = 0;
        if(x>8){                            //横向显示7个数据
            port.left = x-8;
            port.right = x;
        }
        charTem.setMaximumViewport(port);
        charTem.setCurrentViewport(port);
    }
    private void pointUpdate_dust(Float value){
//        float x = 0;
//        PointValue p = new PointValue(position,value);
//        position++;
//        mPointValue.add(p);
//        x = p.getX();
//        Line line = new Line(mPointValue).setColor(Color.parseColor("#f4a00d"));//设置线条颜色
//        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.SQUARE）
//        line.setCubic(true);//曲线是否平滑
//        line.setStrokeWidth(1);//线条的粗细，默认是1
//        line.setFilled(false);//是否填充曲线的面积
//        line.setHasLabels(true);//曲线的数据坐标是否加上备注
//        line.setHasLines(true);//是否用直线显示。如果为false 则没有曲线只有点显示
//        line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示
//        line.setPointColor(Color.parseColor("#f4a00d"));    //设置point点的颜色
//        line.setPointRadius(3);         //设置圆点半径长度
//
//        mLine.add(line);
//        LineChartData data = new LineChartData(mLine);
//        data.setAxisXBottom(axisX);
//        data.setAxisYLeft(axisY);
//        charTem.setLineChartData(data);
//
//        Viewport port = new Viewport(charTem.getMaximumViewport());
//        if (rangeInDefined(value,0,500))
//            port.top = 500;
//        else if (rangeInDefined(value,0,1000))
//            port.top = 1000;
//        else if (rangeInDefined(value,1000,10000))
//            port.top = 10000;
//        else if (rangeInDefined(value,10000,30000))
//            port.top = 30000;
//        else if (rangeInDefined(value,30000,60000))
//            port.top = 60000;
//        else if (rangeInDefined(value,60000,65525))
//            port.top = 65525;
//        port.bottom = 0;
//        if(x>8){                            //横向显示7个数据
//            port.left = x-8;
//            port.right = x;
//        }
//        charTem.setMaximumViewport(port);
//        charTem.setCurrentViewport(port);
    }

    //确定是否在两数之间
    public  boolean rangeInDefined(float current, float min, float max)
    {
        return Math.max(min, current) == Math.min(current, max);
    }

    public void updateChartTemHun(float tem, float hum){        //更新温湿度折线图值
        pointUpdate(tem);
        pointUpdate_hum(hum);
        String temStr = "温度值："+tem;
        SpannableStringBuilder style=new SpannableStringBuilder(temStr+"℃");
        style.setSpan(new ForegroundColorSpan(Color.YELLOW),4,temStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        temshowValue.setText(style);
        temStr = "湿度值："+hum;
        SpannableStringBuilder style1=new SpannableStringBuilder(temStr+"RH");
        style1.setSpan(new ForegroundColorSpan(Color.YELLOW),4,temStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        humshowValue.setText(style1);

    }
    public void updateChartGuang(float guang){              //更新光照度折线图值
        pointUpdate_guang(guang);
        String temStr = "光照度："+guang;
        SpannableStringBuilder style=new SpannableStringBuilder(temStr+"勒克斯");
        style.setSpan(new ForegroundColorSpan(Color.YELLOW),4,temStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        temshowValue.setText(style);
    }
    public void updateChartCo2(float co2){              //更新光照度折线图值
        pointUpdate_co2(co2);
        String temStr = "二氧化碳体积："+co2;
        SpannableStringBuilder style=new SpannableStringBuilder(temStr+"ppm");
        style.setSpan(new ForegroundColorSpan(Color.YELLOW),7,temStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        temshowValue.setText(style);
    }
    public void updateChartO2(float o2){              //更新光照度折线图值
        pointUpdate_o2(o2);
        String temStr = "氧气体积："+o2;
        SpannableStringBuilder style=new SpannableStringBuilder(temStr+"%");
        style.setSpan(new ForegroundColorSpan(Color.YELLOW),5,temStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        temshowValue.setText(style);
    }
    public void updateChartPa(float pa){              //更新光照度折线图值
        pointUpdate_pa(pa);
        String temStr = "大气压："+pa;
        SpannableStringBuilder style=new SpannableStringBuilder(temStr+"hPa");
        style.setSpan(new ForegroundColorSpan(Color.YELLOW),4,temStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        temshowValue.setText(style);
    }
    public void updateChartDust(float dust){              //更新光照度折线图值
        pointUpdate_dust(dust);
        String temStr = "粉尘："+dust;
        SpannableStringBuilder style=new SpannableStringBuilder(temStr+"ppm");
        style.setSpan(new ForegroundColorSpan(Color.YELLOW),3,temStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        temshowValue.setText(style);
    }
    //关闭popwindow 弹窗
    public void closeUIPopWindow(){
//        if (popup != null){
//            popup.dismiss();
//            popup = null;
//        }

    }

    //解析数据,更新折线图
    public void updateChartData(String dataContext){
        try{
            switch (this.sensorType) {
                case "温湿度":
                    String datatemhum = dataContext.substring(9,9 + Integer.parseInt(dataContext.substring(7,9)));
                    if (datatemhum.contains("t")&&datatemhum.contains("h")){
                        int indexhum = datatemhum.indexOf("h");             //datatemhun 值：t+27.1h65.1
                        float temvalue = Float.parseFloat(datatemhum.substring(1,indexhum));
                        float humvalue = Float.parseFloat(datatemhum.substring(1+indexhum));
                        updateChartTemHun(temvalue, humvalue);
                    }
                    break;
                case "光照度":

                    String dataillu = dataContext.substring(9,9 + Integer.parseInt(dataContext.substring(7,9)));
                    updateChartGuang(Float.parseFloat(dataillu));
                    break;
                case "二氧化碳":
                    String dataco2 = dataContext.substring(9,9 + Integer.parseInt(dataContext.substring(7,9)));
                    updateChartCo2(Integer.parseInt(dataco2));
                    break;
                case "氧气":
                    String dataO2 = dataContext.substring(9,9 + Integer.parseInt(dataContext.substring(7,9)));
                    updateChartO2(Float.parseFloat(dataO2));
                    break;
                case "大气压":
                    String dataPa = dataContext.substring(9,9 + Integer.parseInt(dataContext.substring(7,9)));
                    updateChartPa(Float.parseFloat(dataPa));
                    break;
                case "粉尘":
                    //PM1:00100,LZ:0467803,PM2:0166,DLZ:00871
                    String dataDust = dataContext.substring(9,9 + Integer.parseInt(dataContext.substring(7,9)));
                    //目前还不知道怎么解析这个数据，显示在折线图上，是以多条线吗
                    String[] orgl = dataDust.split(",");
                    String pm25Value = orgl[0].split(":")[1];
                    String pm25LzValue = orgl[1].split(":")[1];
                    String pm10Value = orgl[2].split(":")[1];
                    String pm10LzValue = orgl[3].split(":")[1];
                    pm25.setText("PM2.5值：\n"+pm25Value+"ug/m³");
                    pm25lz.setText("PM2.5颗粒数：\n"+pm25LzValue+"个/升");
                    pm10.setText("PM10值：\n"+pm10Value+"ug/m³");
                    pm10lz.setText("PM10颗粒数：\n"+pm10LzValue+"个/升");
//                    updateChartDust(3000);
                    break;
                case "人体红外":
                case "红外对射":
                case "可燃气体":
                case "火焰气体":
                    if (iv_hasornot != null){
                        String hasOrnot = dataContext.substring(9,9 + Integer.parseInt(dataContext.substring(7,9)));
                        iv_hasornot.setImageResource(hasOrnot.equals("0")?R.drawable.hasfalse:R.drawable.hastrue);
                    }
                    break;

            }
        }catch (Exception e){
           e.printStackTrace();
        }
    }

}
