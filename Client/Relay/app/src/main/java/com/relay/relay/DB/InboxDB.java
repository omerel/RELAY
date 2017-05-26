package com.relay.relay.DB;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.SearchUser;
import com.relay.relay.Util.UuidGenerator;
import com.relay.relay.system.RelayMessage;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 23/03/2017.
 */


public class InboxDB {

    final String TAG = "RELAY_DEBUG: "+ InboxDB.class.getSimpleName();
    public static final String REFRESH_INBOX_DB = "relay.BroadcastReceiver.REFRESH_DB";


    final String DB_NAME = "inbox_db";
    final String FORMATTER_DATE = "yyyyMMddHHmmss";
    final String CONTACT_ID = "contact_";
    final String MESSAGE_ID = "message_";
    final String SEARCH_KEY_DELIMITER = "%!%";

    // commands to update inboxDB
    public final int ADD_NEW_MESSAGE_TO_INBOX = 1;
    public final int UPDATE_MESSAGE_STATUS_IN_INBOX = 2;
    public final int DELETE_MESSAGE_FROM_INBOX = 3; // when msg deleted from messageDB
    public static final int DELETE_MESSAGE_CONTENT_FROM_MESSAGE_DB = 4; // when msg deleted from inbox view

    private Database mDatabase = null;
    private Manager mManager = null;
    private UUID mMyId = UUID.randomUUID(); // only for initilaize
    private Messenger mMessengerToDataManager;


    public InboxDB(Context context,Messenger messenger){
        try {
            this.mMessengerToDataManager = messenger;
            mManager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        openDB();
    }


    public Database getDatabase(){return mDatabase;}

    public void getMyNodeIdFromNodesDB(NodesDB nodesDB){
        mMyId = nodesDB.getMyNodeId();
    }

    public boolean updateInboxDB(RelayMessage relayMessage ,int command){

        UUID destinationId = relayMessage.getDestinationId();
        UUID senderId = relayMessage.getSenderId();
        UUID contact = null;


        // in case of user recover, ignore a message that I sent or got that it status is delivered
        if(relayMessage.getStatus() == RelayMessage.STATUS_MESSAGE_DELIVERED &&
                relayMessage.getContent().equals("") &&
                relayMessage.getAttachment() == null )
            return false;

        /// check first if I'm the sender or the destination . in other words, if to to put the msg in the inbox
        if ( mMyId.equals(destinationId) || mMyId.equals(senderId) ){

            if (!mMyId.equals(destinationId))
                contact = destinationId;
            else
                contact = senderId;

            switch (command){
                case ADD_NEW_MESSAGE_TO_INBOX:
                    boolean isMyMessage = senderId.equals(mMyId);
                    // add message to inboxDB( if already exist return false )
                    addMessageItem(relayMessage.getId(),relayMessage.getContent(),contact, relayMessage.getTimeCreated(),isMyMessage);
                    break;
            }
            return true;
        }
        return false;
    }

    /**
     * Create or open the database
     * @return
     */
    public boolean openDB(){
        try {
            mDatabase = mManager.getDatabase(DB_NAME);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private SavedRevision addMessageItem(UUID messageUUID,String msgContent,UUID contactParentUUID,Calendar time,boolean isMyMessage) {

        if (!isMessageExist(messageUUID)){

            Log.e(TAG,"add new msg item");
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("type", "message");
            properties.put("uuid", messageUUID.toString());
            properties.put("contact_parent", contactParentUUID.toString());

            if (isMyMessage){
                Log.e(TAG,"I wrote the msg");
                properties.put("is_my_message", true);
                properties.put("update", false);
                // update parent item with the changes
                updateContactItem(contactParentUUID,msgContent,false,true,false,false);
            }
            else{
                Log.e(TAG,"The msg for me");
                properties.put("is_my_message", false);
                properties.put("update", true);
                // update parent item with the changes
                updateContactItem(contactParentUUID,msgContent,true,true,true,false);
            }
            properties.put("disappear", false);
            properties.put("time", convertCalendarToFormattedString(time));

            String docId = MESSAGE_ID+messageUUID;
            Document document = mDatabase.getDocument(docId);

            try {
                return document.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public SavedRevision addContactItem(UUID contactUUID,final String searchKey){

        if ( !isContactExist(contactUUID) && !contactUUID.equals(mMyId) ){
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("type", "contact");
            properties.put("uuid", contactUUID.toString());
            properties.put("new_messages", false);
            properties.put("updates", true);
            properties.put("disappear", true);
            properties.put("time", convertCalendarToFormattedString(Calendar.getInstance()));
            properties.put("search_key", searchKey);

            String docId = CONTACT_ID+contactUUID;
            Document document = mDatabase.getDocument(docId);

            try {
                return document.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        else{
            // happens when the device got mail from the user before he got its node
            //  need to update the search key or there's a node that need to update
            Document doc = mDatabase.getDocument(CONTACT_ID + contactUUID.toString());
            try {
                doc.update(new Document.DocumentUpdater() {
                    @Override
                    public boolean update(UnsavedRevision newRevision) {
                        Map<String, Object> properties = newRevision.getUserProperties();
                        properties.put("search_key", searchKey);
                        newRevision.setUserProperties(properties);
                        return true;
                    }
                });
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Check if messageUUID exist in database
     * @param messageUUID
     * @return
     */
    private boolean isMessageExist(UUID messageUUID){
        Document doc = mDatabase.getExistingDocument(MESSAGE_ID+messageUUID.toString());
        if (doc == null)
            return false;
        else
            return true;
    }

    /**
     * Check if messageUUID exist in database
     * @param contactUUID
     * @return
     */
    private boolean isContactExist(UUID contactUUID){
        Document doc = mDatabase.getExistingDocument(CONTACT_ID + contactUUID.toString());
        if (doc == null)
            return false;
        else
            return true;
    }


    public boolean updateContactItem(final UUID contactUUID,final String lastMsg, final boolean withNewMessage,
                                     final boolean jumpItem, final boolean toUpdate,final boolean disappear){

        if (isContactExist(contactUUID)){
            Log.e(TAG,"Found contact to update");
            Document doc = mDatabase.getDocument(CONTACT_ID + contactUUID.toString());
            try {
                doc.update(new Document.DocumentUpdater() {
                    @Override
                    public boolean update(UnsavedRevision newRevision) {
                        Map<String, Object> properties = newRevision.getUserProperties();
                        if (!lastMsg.equals(""))
                            properties.put("last_message", lastMsg);
                        properties.put("new_messages", withNewMessage);
                        properties.put("updates", toUpdate);
                        properties.put("disappear", disappear);
                        if (jumpItem) {
                            properties.put("time", convertCalendarToFormattedString(Calendar.getInstance()));
                            properties.put("disappear", false);
                        }
                        newRevision.setUserProperties(properties);
                        Log.e(TAG," contact added to inboxDB");
                        return true;
                    }
                });
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        else{
            Log.e(TAG,"New msg from user that not in my mesh");
            addContactItem(contactUUID,new UuidGenerator().GenerateEmailFromUUID(contactUUID));
            updateContactItem(contactUUID,lastMsg,withNewMessage,jumpItem,toUpdate,disappear);
        }
        return false;
    }

    public boolean setContactSeenByUser(UUID contactUUID){

        if (isContactExist(contactUUID)){
            Document doc = mDatabase.getDocument(CONTACT_ID+contactUUID.toString());
            Map<String, Object> properties = new HashMap<>(doc.getProperties());
            // set off new messages
            properties.put("new_messages", false);
            // set off new updates
            properties.put("updates", false);
            try {
                doc.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    public boolean updateMessageItem(UUID messageUUID){

        if(isMessageExist(messageUUID)){
            Document doc = mDatabase.getDocument(MESSAGE_ID+messageUUID.toString());
            Map<String, Object> properties = new HashMap<>();
            properties.putAll(doc.getProperties());
            properties.put("update", true);

            try {
                doc.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    // Use it when message was deleted from messageDB
    public boolean deleteMessageFromDB(UUID messageUUID){
        if (isMessageExist(messageUUID)){
            try {
                // delete from inbox
                mDatabase.getDocument(MESSAGE_ID+messageUUID.toString()).delete();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    // Use it when node was deleted from nodeDB
    public boolean deleteContactFromDB(UUID contactUUID){
        if (isContactExist(contactUUID)){
            try {
                // delete from inbox
                mDatabase.getDocument(CONTACT_ID+contactUUID.toString()).delete();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    // Use it when message deleted from user on click view
    public boolean deleteMessageFromInbox(UUID messageUUID){
        if (isMessageExist(messageUUID)){
            try {
                // delete from inbox
                mDatabase.getDocument(MESSAGE_ID+messageUUID.toString()).delete();
                //delete message content from messageDB
                sendCommandToDataManager(DELETE_MESSAGE_CONTENT_FROM_MESSAGE_DB,messageUUID.toString());
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public boolean setMessageSeenByUser(UUID messageUUID){

        if(isMessageExist(messageUUID)){
            Document doc = mDatabase.getDocument(MESSAGE_ID+messageUUID.toString());
            Map<String, Object> properties = new HashMap<>();
            properties.putAll(doc.getProperties());
            properties.put("update", false);

            try {
                doc.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    // Use it when click delete in contact - practically only make contact disappear and messages deleted
    public void deleteUserAndConversation(final UUID contactUUID,boolean disappearContact){
        if (isContactExist(contactUUID)){

            Log.e(TAG,"request for deleteUserAndConversation");
            // Create a view and register its map function:
            View messagesView = mDatabase.getView("messagesToDelete"+contactUUID);
            messagesView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    String type = (String) document.get("type");
                    String contactParent = (String) document.get("contact_parent");
                    if ("message".equals(type) && contactParent.equals(contactUUID.toString())) {
                        emitter.emit(document.get("time"), document.get("uuid"));
                    }
                }
            }, "1");

            Log.e(TAG,"how many rows to delete: "+messagesView.getCurrentTotalRows());
            Query query = messagesView.createQuery();
            query.setMapOnly(true);
            QueryEnumerator result = null;
            try {
                result = query.run();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                deleteMessageFromInbox(UUID.fromString((String)row.getValue()));
                Log.e(TAG,"msg deleted from inbox db");
            }

            // disappear user
            if (disappearContact)
                updateContactItem(contactUUID,"",false,false,false,true);

        }
        return;
    }

    public ArrayList<SearchUser> getUserList(DataManager dataManager){

        ArrayList<SearchUser> arrayList = new ArrayList<>();
        String email;
        UUID uuid;
        // Create a view and register its map function:
        View messagesView = mDatabase.getView("userList");

            messagesView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    String type = (String) document.get("type");
                    if ("contact".equals(type)) {
                        emitter.emit(document.get("search_key"), document.get("uuid"));
                    }
                }
            }, "1");

            Query query = messagesView.createQuery();
            query.setMapOnly(true);
            QueryEnumerator result = null;
            try {
                result = query.run();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            for (Iterator<QueryRow> it = result; it.hasNext(); ) {
                QueryRow row = it.next();
                String searchKey = (String)row.getDocument().getProperties().get("search_key");
                String[] searchKeryArray = searchKey.split(SEARCH_KEY_DELIMITER);
                // if the userId in nodesDB and we have the fullName, userName ,  and email
                if (searchKeryArray.length == 3) {
                    String userName = searchKeryArray[1];
                    String fullName = searchKeryArray[0];
                    email = searchKeryArray[2];
                    uuid = UUID.fromString((String) row.getValue());
                    arrayList.add(new SearchUser(fullName, email, userName, uuid));
                } else {
                    // if the userId not in nodesDB and we have only its email
                    uuid = UUID.fromString((String) row.getValue());
                    email = searchKeryArray[0];
                    arrayList.add(new SearchUser("", email, "", uuid));
                }
            }

        return arrayList;
    }

    private String convertCalendarToFormattedString(Calendar cal){

        DateFormat date = new SimpleDateFormat(FORMATTER_DATE);
        return (date.format(cal.getTime()));
    }



    /**
     * Delete database
     * @return
     */
    public boolean deleteDB(){
        try {
            mDatabase.delete();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public void sendCommandToDataManager(int command, String uuid){
        // Send data
        Bundle bundle = new Bundle();
        bundle.putString("uuid", uuid);
        Message msg = Message.obtain(null, command);
        msg.setData(bundle);
        try {
            mMessengerToDataManager.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Error -  sendResultToManager ");
        }
    }


}