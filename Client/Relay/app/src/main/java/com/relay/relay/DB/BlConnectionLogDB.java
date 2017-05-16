package com.relay.relay.DB;

import android.content.Context;
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
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.relay.relay.viewsAndViewAdapters.BluetoothConnectionLogger;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by omer on 14/05/2017.
 */

public class BlConnectionLogDB {

    final String TAG = "RELAY_DEBUG: "+ BlConnectionLogDB.class.getSimpleName();
    final String FORMATTER_DATE = "yyyyMMddHHmmss";
    final String DB_NAME = "bl_connection_logger_db";

    private Database mDatabase = null;
    private Manager mManager = null;

    public BlConnectionLogDB(Context context){
        try {
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

    public Database getDatabase(){return mDatabase;}

    public SavedRevision addToLog(int code, String logMsg) {

        Log.d(TAG,"add new log msg");
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("type", "log");
        properties.put("code", code);
        properties.put("log_msg",logMsg);
        String tStamp = convertCalendarToFormattedString(Calendar.getInstance());
        properties.put("time", tStamp);

        String docId = "Log_"+tStamp;
        Document document = mDatabase.getDocument(docId);

        try {
            return document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return null;
    }




    public ArrayList<BluetoothConnectionLogger> getLogList(){

        ArrayList<BluetoothConnectionLogger> arrayList = new ArrayList<>();
        // Create a view and register its map function:
        View messagesView = mDatabase.getView("logList");

        messagesView.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                String type = (String) document.get("type");
                if ("log".equals(type)) {
                    emitter.emit(document.get("time"), null);
                }
            }
        }, "1");

        Query query = messagesView.createQuery();
        query.setMapOnly(true);
        query.setDescending(true);

        QueryEnumerator result = null;
        try {
            result = query.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            String log = (String)row.getDocument().getProperties().get("log_msg");
            String time = (String)row.getDocument().getProperties().get("time");
            int code = (int)row.getDocument().getProperties().get("code");
            arrayList.add(new BluetoothConnectionLogger(code,time,log));
        }
        return arrayList;
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

    private String convertCalendarToFormattedString(Calendar cal){

        DateFormat date = new SimpleDateFormat(FORMATTER_DATE);
        return (date.format(cal.getTime()));
    }
}
