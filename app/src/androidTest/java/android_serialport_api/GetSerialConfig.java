package android_serialport_api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Bmind on 2017/8/26.
 * 功能描述：获取串口信息，打开串口，传递信息
 */

public class GetSerialConfig {
    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    private SerialPort mSerialPort = null;
    private SerialPort mSerialPort2 = null;

    private SerialPort pcSerialPort = null;
    //获取传进来的串口对象
    public SerialPort getSerialPort(Context context) throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
			/* Read serial port parameters */
            SharedPreferences sp = context.getSharedPreferences("serial_com", MODE_PRIVATE);
            String path = sp.getString("model_com", "");
            Log.d("hello", "路径: "+path);
            String baudrate = sp.getString("model_rate", "");
            /* Check parameters */
            if ( (path.length() == 0) || (baudrate.length() == 0)) {
                throw new InvalidParameterException();
            }
            mSerialPort = new SerialPort(new File(path), Integer.parseInt(baudrate), 0);
        }else{
            Log.d("hello", "该输入串口已经定义");
        }
        return mSerialPort;
    }

    //获取传出去的电脑串口对象
    public SerialPort getPcSerialPort(Context context) throws SecurityException, IOException, InvalidParameterException {
        if (pcSerialPort == null) {
			/* Read serial port parameters */
            SharedPreferences sp = context.getSharedPreferences("serial_com", MODE_PRIVATE);
            String path = sp.getString("pc_com", "");
            String baudrate = sp.getString("pc_rate", "");
            /* Check parameters */
            if ( (path.length() == 0) || (baudrate.length() == 0)) {
                throw new InvalidParameterException();
            }
            pcSerialPort = new SerialPort(new File(path), Integer.parseInt(baudrate), 0);
        }else{
            Log.d("hello", "该输出串口已经定义 ");
        }
        return pcSerialPort;
    }

    //获取蓝牙传进来的串口对象
    public SerialPort gettoothSerialPort(Context context) throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
			/* Read serial port parameters */
            SharedPreferences sp = context.getSharedPreferences("serial_com", MODE_PRIVATE);
            String path = sp.getString("tooth_model_com", "");
            Log.d("hello", "路径: "+path);
            String baudrate = sp.getString("tooth_model_rate", "");
            /* Check parameters */
            if ( (path.length() == 0) || (baudrate.length() == 0)) {
                throw new InvalidParameterException();
            }
            mSerialPort = new SerialPort(new File(path), Integer.parseInt(baudrate), 0);
        }else{
            Log.d("hello", "该输入串口不为空 ");
        }
        return mSerialPort;
    }

    //获取传出去蓝牙的电脑串口对象
    public SerialPort getPctoothSerialPort(Context context) throws SecurityException, IOException, InvalidParameterException {
        if (pcSerialPort == null) {
			/* Read serial port parameters */
            SharedPreferences sp = context.getSharedPreferences("serial_com", MODE_PRIVATE);
            String path = sp.getString("tooth_pc_com", "");
            String baudrate = sp.getString("tooth_pc_rate", "");
            /* Check parameters */
            if ( (path.length() == 0) || (baudrate.length() == 0)) {
                throw new InvalidParameterException();
            }
            pcSerialPort = new SerialPort(new File(path), Integer.parseInt(baudrate), 0);
        }else{
            Log.d("hello", "该串口已经定义 ");
        }
        return pcSerialPort;
    }
    //4G模块 获取传进来的串口对象
    public SerialPort get4GPort(Context context) throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
            //获取保存值
            SharedPreferences pref = context.getSharedPreferences("serial_com", Context.MODE_PRIVATE);
            String path = pref.getString("com_4g","");
            String baudrate = pref.getString("rate_4g","");
            if (path.equals("")||baudrate.equals("")){
                path = "/dev/ttySAC3";                        //默认是串口3,波特率是115200
                baudrate = "115200";
            }
            mSerialPort = new SerialPort(new File(path), Integer.parseInt(baudrate), 0);
        }else{
            Log.d("hello", "该输入串口已经定义");
        }
        return mSerialPort;
    }

    //获取传进来的串口1对象，是为了读取Zigbee数据信息
    public SerialPort getSerialPort2(Context context) throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort2 == null) {
			/* Read serial port parameters */
            SharedPreferences sp = context.getSharedPreferences("serial_com", MODE_PRIVATE);
            String path = sp.getString("model_com", "/dev/ttySAC1");
            Log.d("hello", "路径: "+path);
            String baudrate = sp.getString("model_rate", "115200");
            /* Check parameters */
            if ( (path.length() == 0) || (baudrate.length() == 0)) {
                throw new InvalidParameterException();
            }
            mSerialPort2 = new SerialPort(new File(path), Integer.parseInt(baudrate), 0);
        }else{
            Log.d("hello", "该输入串口已经定义");
        }
        return mSerialPort2;
    }

    //获取PLC传进来的串口对象
    public SerialPort getPlcSerialPort(Context context, String serial, String rate) throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
			if (!serial.equals("")&&!rate.equals("")){
                mSerialPort = new SerialPort(new File(serial), Integer.parseInt(rate), 0);
            }else{
                SharedPreferences sp = context.getSharedPreferences("serial_com", MODE_PRIVATE);
                String path = sp.getString("plc_model_com", "/dev/ttySAC1");
                Log.d("hello", "路径: "+path);
                String baudrate = sp.getString("plc_model_rate", "9600");
                /* Check parameters */
                if ( (path.length() == 0) || (baudrate.length() == 0)) {
                    throw new InvalidParameterException();
                }
                mSerialPort = new SerialPort(new File(path), Integer.parseInt(baudrate), 0);
            }
        }else{
            Log.d("hello", "该输入串口不为空 ");
        }
        return mSerialPort;
    }


    public void closeSerialPort() {         //仅仅关闭接收输入串口
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        if (mSerialPort2 != null){
            mSerialPort2.close();
            mSerialPort2 = null;
        }
    }
    public void closePcSerialPort(){        //仅仅关闭传往电脑的串口
        if (pcSerialPort != null) {
            pcSerialPort.close();
            pcSerialPort = null;
        }
    }
}
