package com.relay.relay.Util;

import java.util.concurrent.TimeUnit;

/**
 * Created by omer on 10/03/2017.
 */

public class TimePerformance {


    private Long start;
    private Long stop;

    public TimePerformance(){}

    public void start(){
        this.start = System.currentTimeMillis();
    }
    public String stop(){
        this.stop = System.currentTimeMillis();
        Long time = stop - start;
         return String.valueOf(time)+ "  millis";
    }

}
