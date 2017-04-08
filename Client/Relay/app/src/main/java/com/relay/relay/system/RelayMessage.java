package com.relay.relay.system;

import android.graphics.Bitmap;

import com.relay.relay.Util.ImageConverter;

import java.io.InputStream;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by omer on 12/02/2017.
 *
 * Message structure in Relay
 */


public class RelayMessage {

    public static int STATUS_MESSAGE_CREATED = 99;
    public static int STATUS_MESSAGE_SENT = 100;
    public static int STATUS_MESSAGE_RECEIVED_IN_SERVER = 200;
    public static int STATUS_MESSAGE_DELIVERED = 300;
    public static int TYPE_MESSAGE_TEXT = 11;
    public static int TYPE_MESSAGE_INCLUDE_ATTACHMENT = 12;

    private UUID mId;
    private Calendar mTimeCreated;
    private int mStatus;
    private UUID mSenderId;
    private UUID mDestinationId;
    private int mType;
    private String mContent;
    private byte[] mAttachment;

    /**
     * Constructor
     * @param mSenderId
     * @param mDestinationId
     * @param mType
     * @param mContent
     */
    public RelayMessage(UUID mSenderId, UUID mDestinationId, int mType, String mContent,byte[] picture) {

        this.mId = UUID.randomUUID(); // TODO check if is it the right generator
        this.mStatus = STATUS_MESSAGE_CREATED;
        this.mTimeCreated = Calendar.getInstance();
        this.mSenderId = mSenderId;
        this.mDestinationId = mDestinationId;
        this.mType = mType;
        this.mContent = mContent;
        this.mAttachment = picture;
    }

    /**
     * Get message id
     * @return
     */
    public UUID getId() {
        return mId;
    }

    /**
     * Get time message created
     * @return
     */
    public Calendar getTimeCreated() {
        return mTimeCreated;
    }

    /**
     * Get message status
     * @return
     */
    public int getStatus() {
        return mStatus;
    }

    /**
     * Get sender id
     * @return
     */
    public UUID getSenderId() {
        return mSenderId;
    }

    /**
     * Get destination id
     * @return
     */
    public UUID getDestinationId() {
        return mDestinationId;
    }

    /**
     * Get message type
     * @return
     */
    public int getType() {
        return mType;
    }

    /**
     * Get message content
     * @return
     */
    public String getContent() {
        return mContent;
    }



    public void deleteContent(){
        mContent = null;
    }

    public void deleteAttachment(){
        mAttachment = null;
    }

    public void setAttachment( byte[] attachment){
        mAttachment = attachment;
    }

    public byte[] getAttachment(){
        return mAttachment;
    }
    /**
     * Get message status
     * @param mStatus
     */
    public void setStatus(int mStatus) {
        this.mStatus = mStatus;
    }


}

