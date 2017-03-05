package com.relay.relay.system;

import android.graphics.Bitmap;

import com.relay.relay.Util.BitmapConvertor;

import java.util.Date;
import java.util.UUID;

/**
 * Created by omer on 12/02/2017.
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

    public UUID getId() {
        return mId;
    }

    public Date getTimeStampNodeDetails() {
        return mTimeStampNodeDetails;
    }

    public void setTimeStampNodeDetails(Date mTimeStampNodeDetails) {
        this.mTimeStampNodeDetails = mTimeStampNodeDetails;
    }

    public Date getTimeStampNodeRelations() {
        return mTimeStampNodeRelations;
    }

    public void setTimeStampNodeRelations(Date mTimeStampNodeRelations) {
        this.mTimeStampNodeRelations = mTimeStampNodeRelations;
    }

    public int getRank() {
        return mRank;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public String getUserName() {
        return mUserName;
    }

    public String getFullName() {
        return mFullName;
    }

    public void setFullName(String mFullName) {
        this.mFullName = mFullName;
    }

    public Bitmap getProfilePicture() {
        return BitmapConvertor.convertBytesToBitmap(mProfilePicture);
    }

    public void setProfilePicture(Bitmap mProfilePicture) {
        this.mProfilePicture = BitmapConvertor.ConvertBitmapToBytes(mProfilePicture);
    }

    public int getResidenceCode() {
        return mResidenceCode;
    }

    public void setResidenceCode(int mResidenceCode) {
        this.mResidenceCode = mResidenceCode;
    }
}
