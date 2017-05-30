package com.relay.relay.DB;

import android.content.Context;

import com.couchbase.lite.Database;
import com.relay.relay.Util.JsonConvertor;
import com.relay.relay.system.HandShakeHistory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by omer on 05/03/2017.
 * HandShakeDB saves all the handshake history that device did.
 */

public class HandShakeDB {


    final String TAG = "RELAY_DEBUG: "+ HandShakeDB.class.getSimpleName();
    final UUID NUM_OF_NODES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b515");
    final int DELAY_BETWEEN_EVENTS = 4;

    private DBManager dbManager;
    final String DB = "handshake_db";

    /**
     * Constructor
     * @param context
     */
    public HandShakeDB(Context context){
        dbManager = new DBManager(DB,context);
        dbManager.openDB();
        if (!dbManager.isKeyExist(NUM_OF_NODES))
            dbManager.putJsonObject(NUM_OF_NODES, JsonConvertor.convertToJson(0));
    }

    /**
     * Add event to DB
     * @param nodeId
     * @return true if success
     */
    public boolean addEventToHandShakeHistoryWith(UUID nodeId,boolean initator){
        HandShakeHistory temp;
        if (!dbManager.isKeyExist(nodeId)) {
            temp = new HandShakeHistory();
            temp.addEvent(initator);
            dbManager.putJsonObject(nodeId,JsonConvertor.convertToJson(temp));
            addNumNodes();
            return true;
        }
        else{
            temp = getHandShakeHistoryWith(nodeId);
            ArrayList<HandShakeHistory.HandShakeEvent> handShakeEvents = temp.getmHandShakeEvents();
            Calendar timestamp1 = handShakeEvents.get(handShakeEvents.size()-1).getTimeStamp();
            Calendar timestamp2 = Calendar.getInstance();
            // check if time pass between two events. makes the history handshake rank be more accurate
            timestamp1.add(Calendar.HOUR,DELAY_BETWEEN_EVENTS);
            if ( timestamp1.before(timestamp2)){
                temp.addEvent(initator);
                dbManager.putJsonObject(nodeId, JsonConvertor.convertToJson(temp));
                return true;
            }
            return false;
        }
    }

    /**
     * Add handshakehistory with uuid to db
     * @param nodeId
     * @return
     */
    public boolean updateHandShakeHistoryWith(UUID nodeId, HandShakeHistory handShakeHistory){

        if (dbManager.isKeyExist(nodeId)){
            dbManager.putJsonObject(nodeId, JsonConvertor.convertToJson(handShakeHistory));
            return true;
        }
        return false;

    }

    /**
     * Get HandShakeHistory of nodeId
     * @param uuid
     * @return Node if found, Null if not found
     */
    public HandShakeHistory getHandShakeHistoryWith(UUID uuid){
        if (dbManager.isKeyExist(uuid)){
            return JsonConvertor.JsonToHandShakeHistory(dbManager.getJsonObject(uuid));
        }
        else
            return null;
    }

    /**
     * delete nodeid from database
     * @param uuid
     * @return
     */
    public boolean deleteNodeId(UUID uuid){
        if (dbManager.isKeyExist(uuid)){
            dbManager.deleteJsonObject(uuid);
            reduceNumNodes();
            return true;
        }
        else
            return false;
    }

    /**
     * Get nodes list
     * @return arraylist
     */
    public ArrayList<UUID> getNodesIdList(){
        ArrayList<UUID> temp = dbManager.getKyes();
        temp.remove(NUM_OF_NODES);
        return temp;

    }

    /**
     * Add to nodes counter
     */
    private void addNumNodes(){
        int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
        num++;
        dbManager.putJsonObject(NUM_OF_NODES,JsonConvertor.convertToJson(num));
    }

    /**
     * Reduce from node counter
     */
    private void reduceNumNodes(){
        int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
        num--;
        if (num >= 0)
            dbManager.putJsonObject(NUM_OF_NODES,JsonConvertor.convertToJson(num));
    }

    /**
     * Get nodes counter
     * @return
     */
    public int getNumNodes() {
        return JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
    }

    /**
     * Delete handshakeDB database
     * @return
     */
    public boolean deleteHandShakeDB(){
        return dbManager.deleteDB();
    }


    public Database getDatabase(){return dbManager.getDatabase();}
}

