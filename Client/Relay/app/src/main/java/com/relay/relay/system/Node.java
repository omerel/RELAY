package com.relay.relay.system;

import android.graphics.Bitmap;

import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.TimeConverter;

import java.io.InputStream;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by omer on 12/02/2017.
 * Node structure in Relay
 */

public class Node {

    private UUID mId;
    private String mTimeStampNodeDetails;
    private String mTimeStampNodeRelations;
    private String mTimeStampRankFromServer;
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
     * @param profilePicture
     * @param mResidenceCode
     */
    public Node(UUID mId, Calendar mTimeStampNodeDetails, Calendar mTimeStampNodeRelations, int mRank,
                String mEmail, String mPhoneNumber, String mUserName, String mFullName,
                byte[] profilePicture, int mResidenceCode,Calendar mTimeStampRankFromServer) {
        this.mId = mId;
        this.mTimeStampNodeDetails = TimeConverter.convertCalendarToFormattedDateString(mTimeStampNodeDetails);
        this.mTimeStampNodeRelations = TimeConverter.convertCalendarToFormattedDateString(mTimeStampNodeRelations);
        this.mRank = mRank;
        this.mEmail = mEmail;
        this.mPhoneNumber = mPhoneNumber;
        this.mUserName = mUserName;
        this.mFullName = mFullName;
        this.mProfilePicture = profilePicture;
        this.mResidenceCode = mResidenceCode;
        this.mTimeStampRankFromServer = TimeConverter.convertCalendarToFormattedDateString(mTimeStampRankFromServer);
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
    public Calendar getTimeStampNodeDetails() {
        return TimeConverter.convertDateStringToFormattedCalendar(mTimeStampNodeDetails);
    }

    /**
     * set tsmp node details
     * @param mTimeStampNodeDetails
     */
    public void setTimeStampNodeDetails(Calendar mTimeStampNodeDetails) {
        this.mTimeStampNodeDetails = TimeConverter.convertCalendarToFormattedDateString(mTimeStampNodeDetails);
    }

    /**
     * get tstmp of last node relations
     * @return
     */
    public Calendar getTimeStampNodeRelations() {
        return TimeConverter.convertDateStringToFormattedCalendar(mTimeStampNodeRelations);
    }

    /**
     * set tstmp node relations
     * @param mTimeStampNodeRelations
     */
    public void setTimeStampNodeRelations(Calendar mTimeStampNodeRelations) {
        this.mTimeStampNodeRelations = TimeConverter.convertCalendarToFormattedDateString(mTimeStampNodeRelations);
    }

    public Calendar getTimeStampRankFromServer() {
        return TimeConverter.convertDateStringToFormattedCalendar(mTimeStampRankFromServer);
    }

    public void setTimeStampRankFromServer(Calendar mTimeStampRankFromServer) {
        this.mTimeStampRankFromServer = TimeConverter.convertCalendarToFormattedDateString(mTimeStampRankFromServer);
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
    public void setRank(int rank,Calendar timeStampRankFromServer) {
         this.mRank = rank;
        setTimeStampRankFromServer(timeStampRankFromServer);
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
        setTimeStampNodeDetails(Calendar.getInstance());
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
        setTimeStampNodeDetails(Calendar.getInstance());
    }

    public void setPhoneNumber(String number) {

        this.mPhoneNumber = number;
        setTimeStampNodeDetails(Calendar.getInstance());
    }

    public void setUserName(String userName) {

        this.mUserName = userName;
        setTimeStampNodeDetails(Calendar.getInstance());
    }

    /**
     * get node's profile picture
     * @return
     */
    public byte[] getProfilePicture() {
        return this.mProfilePicture;
    }

    /**
     * set node's profile picture
     * @param mProfilePicture
     */
    public void setProfilePicture(byte[] mProfilePicture,boolean withTimeChange) {
        this.mProfilePicture = mProfilePicture;
        if(withTimeChange)
            setTimeStampNodeDetails(Calendar.getInstance());
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
        setTimeStampNodeDetails(Calendar.getInstance());
    }
}
