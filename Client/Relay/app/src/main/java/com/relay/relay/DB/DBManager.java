package com.relay.relay.DB;

import android.content.Context;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 01/03/2017.
 * DBManager it's a manage tool for the couchbase database.
 */

public class DBManager {

    final String TAG = "RELAY_DEBUG: "+ DBManager.class.getSimpleName();

    private Database mDatabase = null;
    private Manager mManager = null;
    private String mdbName;

    /**
     *  Creates db manager for specific data base
     * @param dbName
     * @param context
     */
    public DBManager(String dbName, Context context){
        this.mdbName = dbName;

        try {
            mManager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Create or open the database named app
     *
     * @return
     */
    public boolean openDB(){
        try {
            mDatabase = mManager.getDatabase(mdbName);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Put key - value in data base
     * @param key
     * @param jsonObject
     * @return
     */
    public boolean putJsonObject(UUID key, String jsonObject){
        Document document;
        Map<String, Object> properties;
        // create or get doc
        if (isKeyExist(key)){
            // if file exists
            document = mDatabase.getDocument(key.toString());
            // update the old one (its like this to prevent conflicts)
            properties = new HashMap<String, Object>();
            properties.putAll(document.getProperties());
        }
        else{
            document = mDatabase.getDocument(key.toString());
            // create a new properties
            properties = new HashMap<String,Object>();
        }
        // update properties
        properties.put(key.toString(),jsonObject);
        try {
            document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Get value by Key
     * @param key
     * @return
     */
    public String getJsonObject(UUID key){
        Document doc = mDatabase.getDocument(key.toString());
        Map<String, Object> properties = doc.getProperties();
        return (String) properties.get(key.toString());
    }

    public boolean deleteJsonObject(UUID key){
        Document doc = mDatabase.getDocument(key.toString());
        try {
            doc.delete();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Get arraylist of all keys
     * @return
     */
    public ArrayList<UUID> getKyes(){
        ArrayList<UUID> arrayList = new ArrayList<>();
        Query query = mDatabase.createAllDocumentsQuery();
        query.setAllDocsMode(Query.AllDocsMode.ALL_DOCS);
        QueryEnumerator result = null;
        try {
            result = query.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            arrayList.add(UUID.fromString(row.getDocumentId()));
        }
        return arrayList;
    }

    /**
     * Check if key exist in database
     * @param key
     * @return
     */
    public boolean isKeyExist(UUID key){
        Document doc = mDatabase.getExistingDocument(key.toString());
        if (doc == null)
            return false;
        else
            return true;
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
