package com.relay.relay.viewsAndViewAdapters;


/**
 * Created by omer on 14/05/2017.
 */

public class BluetoothConnectionLogger {

    private int flag;
    private String time;
    private String msg;


    public BluetoothConnectionLogger(int flag, String time, String msg){
        this.flag = flag;
        this.time = time;
        this.msg = msg;
    }

    public int getFlagCode(){return flag;}
    public String getTimeWithMsg(){
        return convertTimeToReadableString(time)+" - "+msg+".";
    }


    private String convertTimeToReadableString(String time){
        String year = time.substring(2,4);
        String month = time.substring(4,6);
        String day = time.substring(6,8);
        String hour = time.substring(8,10);
        String min = time.substring(10,12);
        String sec = time.substring(12,14);
        String milsec = time.substring(14,17);
        return day+"/"+month+"/"+year+" "+hour+":"+min+":"+sec+":"+milsec;
    }

}

