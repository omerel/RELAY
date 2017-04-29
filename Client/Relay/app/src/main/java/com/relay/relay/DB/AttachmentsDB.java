package com.relay.relay.DB;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Revision;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.support.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 08/04/2017.
 */

public class AttachmentsDB {

    final String TAG = "RELAY_DEBUG: "+ AttachmentsDB.class.getSimpleName();
    final String DB_NAME = "attachments_db";
    final String ATTACHMENT_ID = "attachment_id_";


    private Database mDatabase = null;
    private Manager mManager = null;


    public AttachmentsDB(Context context){
        try {
            mManager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }
        openDB();
    }

    public Database getDatabase(){return mDatabase;}


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


    public void addAttachment(UUID attachmentUUID, InputStream streamPicture) {

        Document doc = null;
        UnsavedRevision newRev = null;

        Log.e(TAG, "add Attachment");
        if (streamPicture != null) {
            Log.e(TAG, "add new attachment item");
            doc = mDatabase.getDocument(ATTACHMENT_ID + attachmentUUID);
            // Add or update an image to a document as a JPEG attachment:
            if (isAttachmentExist(attachmentUUID)){
                newRev = doc.getCurrentRevision().createRevision();
            }
            else{
                newRev = doc.createRevision();
            }
            newRev.setAttachment("photo.jpg", "image/jpeg", streamPicture);
            try {
                newRev.save();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
    }


    public InputStream getAttachment(UUID attachmentUUID) {

        if (isAttachmentExist(attachmentUUID)) {
            // Load an JPEG attachment from a document into a Drawable:
            Document doc = mDatabase.getDocument(ATTACHMENT_ID + attachmentUUID);
            Revision rev = doc.getCurrentRevision();
            Attachment att = rev.getAttachment("photo.jpg");
            if (att != null) {
                InputStream is = null;
                try {
                    is = att.getContent();
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }
                return is;
            }
        }
        return null;
    }

    public void deleteAttachment(UUID attachmentUUID) {

        if (isAttachmentExist(attachmentUUID)) {
            // Remove an attachment from a document:
            Document doc = mDatabase.getDocument(ATTACHMENT_ID + attachmentUUID);
            UnsavedRevision newRev = doc.getCurrentRevision().createRevision();
            newRev.removeAttachment("photo.jpg");
            // (You could also update newRev.properties while you're here)
            try {
                newRev.save();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean isAttachmentExist(UUID attachmentUUID){
        Document doc = mDatabase.getExistingDocument(ATTACHMENT_ID+attachmentUUID);
        if (doc == null)
            return false;
        else
            return true;
    }

    public boolean deleteDB(){
        try {
             mDatabase.delete();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return true;
    }


}
