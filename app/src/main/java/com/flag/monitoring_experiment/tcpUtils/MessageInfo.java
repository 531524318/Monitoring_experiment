package com.flag.monitoring_experiment.tcpUtils;

/**
 * Created by Bmind on 2018/6/26.
 */

public class MessageInfo {
    private String infoType;        //"exit","link"，"data"
    private String context;
    //{'type':'temhun','wifiip':'','zigbeeip':'','wifistatue':'false','zigbeestatue':'false'}
    public MessageInfo(String infoType, String context) {
        this.infoType = infoType;
        this.context = context;
    }

    public String getContext() {
        return context;
    }

    public String getInfoType() {
        return infoType;
    }
}
