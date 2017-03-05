package com.relay.relay.DB;
import android.content.Context;

import com.relay.relay.Util.JsonConvertor;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by omer on 01/03/2017.
 */

public class MessagesDB {
    final String TAG = "RELAY_DEBUG: "+ MessagesDB.class.getSimpleName();
    final UUID NUM_OF_MESSAGES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b515");
    private DBManager dbManager;
    final String DB = "messages_db";

    public MessagesDB(Context context){
        dbManager = new DBManager(DB,context);
        dbManager.openDB();
        dbManager.putJsonObject(NUM_OF_MESSAGES, JsonConvertor.ConvertToJson(0));
    }

    public boolean addMessage(RelayMessage message){
        if (!dbManager.isKeyExist(message.getId())) {
            dbManager.putJsonObject(message.getId(), JsonConvertor.ConvertToJson(message));
            addNumMessages();
            return true;
        }
        else{
            dbManager.putJsonObject(message.getId(), JsonConvertor.ConvertToJson(message));
            return true;
        }
    }

    public RelayMessage getMessage(UUID uuid){
        if (dbManager.isKeyExist(uuid))
            return JsonConvertor.JsonToRelayMessage(dbManager.getJsonObject(uuid));
        else
            return null;
    }
    public boolean deleteMessage(UUID uuid){
        if (dbManager.isKeyExist(uuid)){
            dbManager.deleteJsonObject(uuid);
            reduceNumNodes();
            return true;
        }
        else
            return false;
    }
    public ArrayList<UUID> getMessagesIdList(){
        ArrayList<UUID> temp = dbManager.getKyes();
        temp.remove(NUM_OF_MESSAGES);
        return temp;

    }
    public void addNumMessages(){
        int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_MESSAGES));
        num++;
        dbManager.putJsonObject(NUM_OF_MESSAGES,JsonConvertor.ConvertToJson(num));
    }

    public void reduceNumNodes(){
        int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_MESSAGES));
        num--;
        if (num >= 0)
            dbManager.putJsonObject(NUM_OF_MESSAGES,JsonConvertor.ConvertToJson(num));
    }

    public boolean deleteMessageDB(){
        return dbManager.deleteDB();
    }

    public int getNumNodes() {
        return JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_MESSAGES));
    }
}
