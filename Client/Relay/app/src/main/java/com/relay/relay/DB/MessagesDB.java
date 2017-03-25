package com.relay.relay.DB;
import android.content.Context;

import com.relay.relay.Util.JsonConvertor;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by omer on 01/03/2017.
 * MessageDB saves all the messages that device received and sent
 */

public class MessagesDB {
    final String TAG = "RELAY_DEBUG: "+ MessagesDB.class.getSimpleName();
    final UUID NUM_OF_MESSAGES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b515");

    private DBManager dbManager;
    final String DB = "messages_db";
    private InboxDB mInboxDB;

    /**
     * Constructor. gets Context. and open the database.
     * @param context
     */
    public MessagesDB(Context context,InboxDB inboxDB){
        dbManager = new DBManager(DB,context);
        dbManager.openDB();
        dbManager.putJsonObject(NUM_OF_MESSAGES, JsonConvertor.convertToJson(0));
        this.mInboxDB = inboxDB;
    }


    /**
     * Add message to data base
     * @param message
     * @return true if success
     */
    public boolean addMessage(RelayMessage message){
        if (!isMessageExist(message.getId())) {
            dbManager.putJsonObject(message.getId(), JsonConvertor.convertToJson(message));
            addNumMessages();
            mInboxDB.updateInboxDB(message, mInboxDB.ADD_NEW_MESSAGE_TO_INBOX);
            return true;
        }
        else{
            dbManager.putJsonObject(message.getId(), JsonConvertor.convertToJson(message));
            mInboxDB.updateInboxDB(message, mInboxDB.UPDATE_MESSAGE_STATUS_IN_INBOX);
            return true;
        }
    }

    /**
     * Get message from data base
     * @param uuid
     * @return true if success
     */
    public RelayMessage getMessage(UUID uuid){
        if (dbManager.isKeyExist(uuid))
            return JsonConvertor.JsonToRelayMessage(dbManager.getJsonObject(uuid));
        else
            return null;
    }


    public boolean isMessageExist(UUID uuid){
        return dbManager.isKeyExist(uuid);
    }

    /**
     * Delete message from database
     * @param uuid
     * @return true if success. false if uuid not exist
     */
    public boolean deleteMessage(UUID uuid){
        if (dbManager.isKeyExist(uuid)){
            dbManager.deleteJsonObject(uuid);
            reduceNumNodes();
            mInboxDB.updateInboxDB(getMessage(uuid), mInboxDB.DELETE_MESSAGE_FROM_INBOX);
            return true;
        }
        else
            return false;
    }

    /**
     * return list of all messages id's
     * @return ArrayList
     */
    public ArrayList<UUID> getMessagesIdList(){
        ArrayList<UUID> temp = dbManager.getKyes();
        temp.remove(NUM_OF_MESSAGES);
        return temp;

    }

    /**
     * count num of messages in database
     */
    public void addNumMessages(){
        int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_MESSAGES));
        num++;
        dbManager.putJsonObject(NUM_OF_MESSAGES,JsonConvertor.convertToJson(num));
    }

    /**
     * reduce num of messages from database
     */
    public void reduceNumNodes(){
        int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_MESSAGES));
        num--;
        if (num >= 0)
            dbManager.putJsonObject(NUM_OF_MESSAGES,JsonConvertor.convertToJson(num));
    }

    /**
     * Delete database
     * @return
     */
    public boolean deleteMessageDB(){
        return dbManager.deleteDB();
    }

    /**
     * Get messages counter
     * @return
     */
    public int getNumNodes() {
        return JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_MESSAGES));
    }
}
