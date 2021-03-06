package com.relay.relay.system;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by omer on 05/03/2017.
 * HandShakeHistory saves all the hand shake events with devices, and gives a handshake Rank
 * that define how much of depth in the degree will be in the hand shake.
 */

public class HandShakeHistory {


    final int DEFAULT_RANK = 2;

    private int mHandShakeRank;
    private int mHandShakeCounter;
    private ArrayList<HandShakeEvent> mHandShakeEvents;
    private ArrayList<HandShakeEvent> mHandShakeEventLog;

    public HandShakeHistory(){

        this.mHandShakeRank = DEFAULT_RANK;
        this.mHandShakeCounter = 0;
        this.mHandShakeEvents = new ArrayList<>();
        this.mHandShakeEventLog = new ArrayList<>();
    }


    /**
     * Calculate HandShake rank define what the current rank will be.
     * @return
     */
    private boolean calculateHandShakeRank(){

        // TODO examine this approach
        if ( mHandShakeCounter >= 0 && mHandShakeCounter < 5)
            mHandShakeRank = 2;
        if ( mHandShakeCounter >= 5 && mHandShakeCounter < 10)
            mHandShakeRank = 3;
        if ( mHandShakeCounter >= 10 && mHandShakeCounter < 15)
            mHandShakeRank = 4;
        if ( mHandShakeCounter >= 15 && mHandShakeCounter < 20)
            mHandShakeRank = 5;
        if ( mHandShakeCounter >= 20)
            mHandShakeRank = 6;
        return true;
    }

    /**
     * get mHandShakeCounter
     * @return
     */
    public int getmHandShakeCounter() {
        return mHandShakeCounter;
    }

    /**
     * get mHandShakeEvents
     * @return
     */
    public ArrayList<HandShakeEvent> getmHandShakeEvents() {
        return mHandShakeEvents;
    }

    /**
     * get mHandShakeEventLog
     * @return
     */
    public ArrayList<HandShakeEvent> getmHandShakeEventLog() {
        return mHandShakeEventLog;
    }

    /**
     * get mHandShakeRank
     * @return
     */
    public int getmHandShakeRank() {
        return mHandShakeRank;
    }

    /**
     * add event to HandShakeEvents
     * @return
     */
    public boolean addEvent(boolean initiator){
        mHandShakeEvents.add(new HandShakeEvent(initiator));
        mHandShakeCounter++;
        calculateHandShakeRank();
        return true;
    }

    /**
     * clean all the events that happened before @hour ago
     * @param hour
     * @return
     */
    public boolean moveOldHandShakEventsToLog(int hour){

        ArrayList<HandShakeEvent> tempEvents = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR,-hour);
        for ( HandShakeEvent handShakeEvent : mHandShakeEvents){
            // reduce all the events that created before weeks from now
            if (handShakeEvent.timeStamp.after(cal)){
                tempEvents.add(handShakeEvent);
            }
            else{
                // add event to log
                mHandShakeEventLog.add(handShakeEvent);
            }
        }
        // update mHandShakeEvents
        mHandShakeEvents = tempEvents;
        calculateHandShakeRank();
        return true;
    }

    /**
     * delete HandShakeEventLog
     * @return
     */
    public boolean deleteHandShakeEventLog(){
        mHandShakeEventLog = new ArrayList<>();
        return true;
    }

    /**
     *  HandShakeEvent - saves the place and the time where the handshake with the device occurred.
     *  TODO for now the geo location is not usable;
     */
     public class HandShakeEvent{
        private String geoLocation;
        private Calendar timeStamp;
        private boolean initiator;

        public HandShakeEvent(boolean initiator){
            this.geoLocation = "IL";
            this.timeStamp = Calendar.getInstance();
            this.initiator = initiator;
        }

        public String getGeoLocation() {
            return geoLocation;
        }

        public Calendar getTimeStamp() {
            return timeStamp;
        }

        public boolean getInitiator() {return  initiator;}
    }
}
