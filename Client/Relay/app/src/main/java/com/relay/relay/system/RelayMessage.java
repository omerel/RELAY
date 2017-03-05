package com.relay.relay.system;

import android.graphics.Bitmap;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 12/02/2017.
 */


public class RelayMessage {

    public static int STATUS_MESSAGE_CREATED = 99;
    public static int STATUS_MESSAGE_SENT = 100;
    public static int STATUS_MESSAGE_RECEIVED_IN_SERVER = 200;
    public static int STATUS_MESSAGE_DELIVERED = 300;
    public static int TYPE_MESSAGE_TEXT = 11;
    public static int TYPE_MESSAGE_INCLUDE_ATTACHMENT = 12;
    public static int TYPE_ATTACHMENT_BITMAP = 13;

    private UUID mId;
    private Date mTimeCreated;
    private int mStatus;
    private UUID mSenderId;
    private UUID mDestinationId;
    private int mType;
    private String mContent;
    private Map<UUID,Attachment> mAttachments;

    public RelayMessage(UUID mSenderId, UUID mDestinationId, int mType, String mContent) {

        this.mId = UUID.randomUUID(); // TODO check if is it the right generator
        this.mStatus = STATUS_MESSAGE_CREATED;
        this.mTimeCreated = Calendar.getInstance().getTime();
        this.mSenderId = mSenderId;
        this.mDestinationId = mDestinationId;
        this.mType = mType;
        this.mContent = mContent;
        this.mAttachments = new HashMap<>();
    }

    public UUID getId() {
        return mId;
    }

    public Date getTimeCreated() {
        return mTimeCreated;
    }

    public int getStatus() {
        return mStatus;
    }

    public UUID getSenderId() {
        return mSenderId;
    }

    public UUID getDestinationId() {
        return mDestinationId;
    }

    public int getType() {
        return mType;
    }

    public String getContent() {
        return mContent;
    }

    public Attachment[] getAttachments() {
        if ( mAttachments.size() > 0)
            return (Attachment[]) mAttachments.values().toArray();
        return null;
    }

    public Attachment getAttachment(UUID id) {
        if (mAttachments.containsKey(id))
            return  mAttachments.get(id);
        return null;
    }

    public boolean addAttachment(Object content,int type){

        if ( content == null )
            // no content
            return false;

        if ( type == TYPE_ATTACHMENT_BITMAP){
            Attachment tempAttachment =
                    new Attachment(BitmapConvertor.ConvertBitmapToBytes((Bitmap)content),type);
            mAttachments.put(tempAttachment.getId(),tempAttachment);
            return true;
        }
        // not such of type;
        return false;
    }

    public void setStatus(int mStatus) {
        this.mStatus = mStatus;
    }

    private class Attachment{
        private UUID mId;
        private int mType;
        private Object mContent;

        public Attachment(Object mContent, int mType) {
            this.mContent = mContent;
            this.mType = mType;
            this.mId = UUID.randomUUID(); // TODO check if is it the right generator
        }

        public UUID getId() {
            return mId;
        }

        public int getType() {
            return mType;
        }

        public Bitmap getContent() {

            if ( mType == TYPE_ATTACHMENT_BITMAP)
                return (Bitmap) BitmapConvertor.convertBytesToBitmap((byte[])mContent);

            return null;
        }
    }
}

