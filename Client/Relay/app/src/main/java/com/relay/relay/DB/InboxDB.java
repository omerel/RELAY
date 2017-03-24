package com.relay.relay.DB;

import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 23/03/2017.
 */

public class InboxDB {

    final String TAG = "RELAY_DEBUG: "+ InboxDB.class.getSimpleName();
    final String DB_NAME = "inbox_db";
    final String FORMATTER_DATE = "yyyyMMddhhmmss";
    final String CONTACT_ID = "contact_";
    final String MESSAGE_ID = "message_";

    private Database mDatabase = null;
    private Manager mManager = null;
    private UUID mMyId;
    private MessagesDB messagesDB;


    public InboxDB(Context context,UUID myId){
        try {
            this.mMyId = myId;
            mManager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        openDB();
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


    public SavedRevision addMessageItem(UUID messageUUID,UUID contactParentUUID) {

        if (!isMessageExist(messageUUID)){

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("type", "message");
            properties.put("uuid", messageUUID.toString());
            properties.put("contact_parent", contactParentUUID.toString());
            properties.put("is_new_message", true);
            properties.put("update", true);
            properties.put("disappear", false);
            properties.put("time", convertCalendarToFormattedString(messagesDB.getMessage(messageUUID).getTimeCreated()));

            String docId = UUID.randomUUID().toString();
            Document document = mDatabase.getDocument(docId);

            // update parent item with the changes
            updateContactItem(contactParentUUID,true);

            try {
                return document.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public SavedRevision addContactItem(UUID contactUUID){

        if (!isContactExist(contactUUID)){
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("type", "contact");
            properties.put("uuid", contactUUID.toString());
            properties.put("new_messages", true);
            properties.put("updates", true);
            properties.put("time", convertCalendarToFormattedString(Calendar.getInstance()));

            String docId = CONTACT_ID+UUID.randomUUID();
            Document document = mDatabase.getDocument(docId);

            try {
                return document.putProperties(properties);
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
        Document doc = mDatabase.getExistingDocument(CONTACT_ID+contactUUID.toString());
        if (doc == null)
            return false;
        else
            return true;
    }

    public boolean updateContactItem(UUID contactUUID, boolean withNewMessage){

        if (isContactExist(contactUUID)){
            Document doc = mDatabase.getDocument(CONTACT_ID+contactUUID.toString());
            Map<String, Object> properties = doc.getProperties();
            if (withNewMessage)
                properties.put("new_messages", true);
            properties.put("updates", true);
            properties.put("time", convertCalendarToFormattedString(Calendar.getInstance()));

            try {
                doc.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public boolean setContactSeenByUser(UUID contactUUID){

        if (isContactExist(contactUUID)){
            Document doc = mDatabase.getDocument(CONTACT_ID+contactUUID.toString());
            Map<String, Object> properties = doc.getProperties();
            properties.put("new_messages", false);
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
            Map<String, Object> properties = doc.getProperties();
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

    public boolean setMessageSeenByUser(UUID messageUUID){

        if(isMessageExist(messageUUID)){
            Document doc = mDatabase.getDocument(MESSAGE_ID+messageUUID.toString());
            Map<String, Object> properties = doc.getProperties();
            properties.put("update", false);
            properties.put("is_new_message", false);

            try {
                doc.putProperties(properties);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }



    private String convertCalendarToFormattedString(Calendar cal){

        DateFormat date = new SimpleDateFormat(FORMATTER_DATE);
        return (date.format(cal));
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

}