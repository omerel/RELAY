package com.relay.relay.system;

import android.graphics.Bitmap;

import com.relay.relay.Util.BitmapConvertor;

import java.util.Date;
import java.util.UUID;

/**
 * Created by omer on 12/02/2017.
 * Node structure in Relay
 */

public class Node {

    private UUID mId;
    private Date mTimeStampNodeDetails;
    private Date mTimeStampNodeRelations;
    private int mRank;
    private String mEmail;
    private String mPhoneNumber;
    private String mUserName;
    private String mFullName;
    private byte[] mProfilePicture;
    private int mResidenceCode;

    /**
     * Constructor
     * @param mId
     * @param mTimeStampNodeDetails
     * @param mTimeStampNodeRelations
     * @param mRank
     * @param mEmail
     * @param mPhoneNumber
     * @param mUserName
     * @param mFullName
     * @param mProfilePicture
     * @param mResidenceCode
     */
    public Node(UUID mId, Date mTimeStampNodeDetails, Date mTimeStampNodeRelations, int mRank,
                String mEmail, String mPhoneNumber, String mUserName, String mFullName,
                Bitmap mProfilePicture, int mResidenceCode) {
        this.mId = mId;
        this.mTimeStampNodeDetails = mTimeStampNodeDetails;
        this.mTimeStampNodeRelations = mTimeStampNodeRelations;
        this.mRank = mRank;
        this.mEmail = mEmail;
        this.mPhoneNumber = mPhoneNumber;
        this.mUserName = mUserName;
        this.mFullName = mFullName;
        this.mProfilePicture = BitmapConvertor.ConvertBitmapToBytes(mProfilePicture);
        this.mResidenceCode = mResidenceCode;
    }

    /**
     * Get node id
     * @return
     */
    public UUID getId() {
        return mId;
    }

    /**
     * Get tsmp of last changes in node details
     * @return
     */
    public Date getTimeStampNodeDetails() {
        return mTimeStampNodeDetails;
    }

    /**
     * set tsmp node details
     * @param mTimeStampNodeDetails
     */
    public void setTimeStampNodeDetails(Date mTimeStampNodeDetails) {
        this.mTimeStampNodeDetails = mTimeStampNodeDetails;
    }

    /**
     * get tstmp of last node relations
     * @return
     */
    public Date getTimeStampNodeRelations() {
        return mTimeStampNodeRelations;
    }

    /**
     * set tstmp node relations
     * @param mTimeStampNodeRelations
     */
    public void setTimeStampNodeRelations(Date mTimeStampNodeRelations) {
        this.mTimeStampNodeRelations = mTimeStampNodeRelations;
    }

    /**
     * Get node rank
     * @return
     */
    public int getRank() {
        return mRank;
    }

    /**
     * Set node rank
     * @param rank
     */
    public void setRank(int rank) {
         this.mRank = rank;
    }

    /**
     * Get node's email
     * @return
     */
    public String getEmail() {
        return mEmail;
    }

    /**
     * set node's email
     * @param mEmail
     */
    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    /**
     * get node's phone number
     * @return
     */
    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    /**
     * get node's user name
     * @return
     */
    public String getUserName() {
        return mUserName;
    }

    /**
     * get node's full name
     * @return
     */
    public String getFullName() {
        return mFullName;
    }

    /**
     * set node's name
     * @param mFullName
     */
    public void setFullName(String mFullName) {
        this.mFullName = mFullName;
    }

    /**
     * get node's profile picture
     * @return
     */
    public Bitmap getProfilePicture() {
        return BitmapConvertor.convertBytesToBitmap(mProfilePicture);
    }

    /**
     * set node's profile picture
     * @param mProfilePicture
     */
    public void setProfilePicture(Bitmap mProfilePicture) {
        this.mProfilePicture = BitmapConvertor.ConvertBitmapToBytes(mProfilePicture);
    }

    /**
     * get Residence code
     * @return
     */
    public int getResidenceCode() {
        return mResidenceCode;
    }

    /**
     * set Residence code
     * @param mResidenceCode
     */
    public void setResidenceCode(int mResidenceCode) {
        this.mResidenceCode = mResidenceCode;
    }
}