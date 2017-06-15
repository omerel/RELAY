package com.relay.relay.SubSystem;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.Bluetooth.BLConstants;
import com.relay.relay.Bluetooth.BluetoothConnected;
import com.relay.relay.Util.DataTransferred;
import com.relay.relay.Util.JsonConvertor;
import com.relay.relay.Util.TimePerformance;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 12/12/2016.
 * HandShake using bluetooth connection
 */


public class HandShake implements BLConstants {

    private final String TAG = "RELAY_DEBUG: "+ HandShake.class.getSimpleName();;


    // commands
    private final int STEP_1_METADATA = 1;
    private final int FINISH_STEP_1 = 2;
    private final int STEP_2_UPDATE_NODES_AND_RELATIONS = 3;
    private final int STEP_3_TEXT_MESSAGES = 5;
    private final int STEP_3_SKIP = 55;
    private final int STEP_4_OBJECT_MESSAGE = 6;
    private final int ACK_OBJECT_STEP_4 = 9;
    private final int FINISH_STEP_4 = 7;
    private final int FINISH = 8;

    private Messenger mMessenger;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothConnected mBluetoothConnected;
    private boolean mInitiator;
    private DataManager mDataManager;
    private DataTransferred mDataTransferred;
    private Node mMyNode;
    private Handler watchDogHandler;
    private Runnable stepWatchDogRunnable;

    private DataTransferred.Metadata metadata;
    private DataTransferred.Metadata receivedMetadata;
    private Map<UUID,DataTransferred.KnownMessage> receivedKnownMessage;
    private DataTransferred.UpdateNodeAndRelations updateNodeAndRelations;
    private Map<UUID,DataTransferred.KnownRelations> knownRelations;
    private Map<UUID,DataTransferred.KnownRelations> receivedKnownRelations;
    private DataTransferred.UpdateNodeAndRelations receivedUpdateNodeAndRelations;
    private ArrayList<RelayMessage> textMessagesToSend;
    private ArrayList<RelayMessage> objectMessagesToSend;
    private int finalDegree; // the actual degree for getting data from the other device
    private Map<UUID,Node> newNodeIdList; // store all the  new nodes that sent to the device. used to update messages status
    private TimePerformance timePerformance = new TimePerformance();

    private String step;

    // attachments sent on by one because of their size
    private int attachmentsCounter;


    public HandShake(BluetoothSocket bluetoothSocket,
                     Messenger messenger, boolean initiator, Context context,
                     DataManager dataManager,DataTransferred.Metadata metadata){

        this.mMessenger = messenger;
        this.mBluetoothSocket = bluetoothSocket;
        this.mInitiator = initiator;
        this.mBluetoothConnected = new BluetoothConnected(mBluetoothSocket,messenger);
        this.mDataManager = dataManager;
        this.mDataTransferred = new DataTransferred(dataManager.getGraphRelations(),
                dataManager.getNodesDB(),dataManager.getMessagesDB());
        this.textMessagesToSend = new ArrayList<>();
        this.objectMessagesToSend = new ArrayList<>();
        this.metadata = metadata;
        this.mBluetoothConnected.start();
        this.step = "";

        // set watchdog
        this.stepWatchDogRunnable = new Runnable() {
            @Override
            public void run() {
                mBluetoothConnected.cancel();
                Log.e(TAG,"Error - watchDogHandler");
                sendMessageToManager(FAILED_DURING_HAND_SHAKE,"Error - WatchDogHandler. After sending: "+step);
            }
        };

        // refresh db
        mDataManager.closeAllDataBase();
        mDataManager.openAllDataBase();

        //start hand shake
        startHandshake();

    }


    /**
     * if handshake is stuck close it after period of time
     * @param time
     */
    private void handShakeWatchDog(int time,String step){
        watchDogHandler = new Handler();
        watchDogHandler.postDelayed(stepWatchDogRunnable, time);
    }


    /**
     * Start handshake process
     */
    private void startHandshake() {

        // set my node
        mMyNode = mDataManager.getNodesDB().getNode(mDataManager.getMyUuid());

        attachmentsCounter = 0;
        timePerformance.start();
        this.metadata = mDataTransferred.createMetaData();
        Log.e(TAG,"TIME TO : "+"createMetaData"+" "+ timePerformance.stop());
        this.knownRelations = metadata.getKnownRelationsList();
        step =  "STEP_1_METADATA";
        if(mInitiator){
            sendPacket(STEP_1_METADATA,JsonConvertor.convertToJson(metadata));
            handShakeWatchDog(WATCHDOG_TIMER,"startHandshake");
        }
        else
            // start watchdog in this step
            handShakeWatchDog(WATCHDOG_TIMER*2,"startHandshake");
    }

    /**
     * Getter of incoming messages from one of the BluetoothConnected
     */

    public void getPacket(String jsonPacket){
        int step = 0; // for debugging
        Log.e(TAG,"JsonString received size is : "+jsonPacket.length());
        JsonConvertor.isJSONValid(jsonPacket);
        try {
            int command = JsonConvertor.getCommand(jsonPacket);
            switch (command) {
                case STEP_1_METADATA:
                    step=1;//1
                    Log.e(TAG, "STEP_1_METADATA. Initiator: "+mInitiator );
                    // reset watch dog on this step;
                    watchDogHandler.removeCallbacks(stepWatchDogRunnable);
                    // receive meta data
                    receivedMetadata = JsonConvertor.getMetadataFromJsonContent(jsonPacket);
                    receivedKnownMessage = receivedMetadata.getKnownMessagesList();
                    receivedKnownRelations = receivedMetadata.getKnownRelationsList();
                    step=2;//2
                    timePerformance.start();
                    checkSyncNodeRankBeforeHandShake();
                    Log.e(TAG, "TIME TO : " + "checkSyncNodeRankBeforeHandShake" + " " + timePerformance.stop());
                    finalDegree = CalculateFinalRank();
                    timePerformance.start();
                    step=3;//3
                    updateNodeAndRelations = createUpdateNodeAndRelations(finalDegree);
                    step=31;//3
                    Log.e(TAG, "TIME TO : " + "createUpdateNodeAndRelations" + " " + timePerformance.stop());
                    if (!mInitiator) {
                        step=4;//4
                        sendPacket(STEP_1_METADATA, JsonConvertor.convertToJson(metadata));
                    } else {
                        step=5;//5
                        sendPacket(STEP_2_UPDATE_NODES_AND_RELATIONS, JsonConvertor.convertToJson(updateNodeAndRelations));
                    }
                    // start watchdog in this step
                    if (!mInitiator) {
                        handShakeWatchDog(WATCHDOG_TIMER,"STEP_1_METADATA");
                    } else {
                        handShakeWatchDog(WATCHDOG_TIMER,"STEP_2_UPDATE_NODES_AND_RELATIONS");
                    }

                    break;
                case STEP_2_UPDATE_NODES_AND_RELATIONS:
                    step=6;///6
                    Log.e(TAG, "STEP_2_UPDATE_NODES_AND_RELATIONS. Initiator: "+mInitiator );
                    // reset watch dog on this step;
                    watchDogHandler.removeCallbacks(stepWatchDogRunnable);
                    receivedUpdateNodeAndRelations =
                            JsonConvertor.getUpdateNodeAndRelationsFromJsonContent(jsonPacket);
                    step=7;//7
                    timePerformance.start();
                    updateNodeAndRelations(receivedUpdateNodeAndRelations);
                    Log.e(TAG, "TIME TO : " + "updateNodeAndRelations" + " " + timePerformance.stop());
                    timePerformance.start();
                    step=8;//8
                    updateMessagesAndCreateMessagesListToSend();
                    Log.e(TAG, "TIME TO : " + "updateMessagesAndCreateMessagesListToSend" + " " + timePerformance.stop());

                    if (!mInitiator) {
                        step=9;//9
                        sendPacket(STEP_2_UPDATE_NODES_AND_RELATIONS, JsonConvertor.convertToJson(updateNodeAndRelations));
                    } else {
                        step=10;//10
                        Log.e(TAG,"textMessagesToSend size: "+textMessagesToSend.size());
                        // if there is no new messages to update, skip step
                        if(textMessagesToSend.size() == 0)
                            sendPacket(STEP_3_SKIP, new String("DUMMY"));
                        else
                            sendPacket(STEP_3_TEXT_MESSAGES, JsonConvertor.convertToJson(textMessagesToSend));
                    }
                    // start watchdog in this step
                    if (!mInitiator) {
                        handShakeWatchDog(WATCHDOG_TIMER,"STEP_2_UPDATE_NODES_AND_RELATIONS");
                    } else {
                        handShakeWatchDog(WATCHDOG_TIMER,"STEP_3_TEXT_MESSAGES or STEP_3_SKIP");
                    }
                    break;

                case STEP_3_SKIP:
                    Log.e(TAG, "STEP_3_SKIP. Initiator: "+mInitiator );
                    // reset watch dog on this step;
                    watchDogHandler.removeCallbacks(stepWatchDogRunnable);
                    if (!mInitiator) {
                        step=15;//15
                        // if there is no new messages to update, skip step
                        if(textMessagesToSend.size() == 0)
                            sendPacket(STEP_3_SKIP, new String("DUMMY"));
                        else
                            sendPacket(STEP_3_TEXT_MESSAGES, JsonConvertor.convertToJson(textMessagesToSend));
                    } else {
                        timePerformance.start();
                        step=16;//16
                        updateSentTextMessages();
                        Log.e(TAG, "TIME TO : " + "updateSentTextMessages" + " " + timePerformance.stop());
                        timePerformance.start();
                        // send first attachment if exist
                        if (objectMessagesToSend.size() > 0) {
                            step=17;//17
                            Log.e(TAG, "there are  "+objectMessagesToSend.size()+ " objects to send, sending " +attachmentsCounter +" from "+objectMessagesToSend.size() );
                            sendPacket(STEP_4_OBJECT_MESSAGE, JsonConvertor.convertToJson(objectMessagesToSend.get(attachmentsCounter)));
                            attachmentsCounter ++;
                        } else{
                            step=18;//18
                            Log.e(TAG, "there are  "+objectMessagesToSend.size()+ " objects to send, sending " +attachmentsCounter +" from "+objectMessagesToSend.size() );
                            Log.e(TAG, "TIME TO : " + "objectMessagesToSend" + " " + timePerformance.stop());
                            sendPacket(FINISH_STEP_4, new String("DUMMY"));
                        }
                    }
                    // start watchdog in this step
                    if (!mInitiator) {
                        handShakeWatchDog(WATCHDOG_TIMER,"STEP_3_TEXT_MESSAGES or STEP_3_SKIP");
                    } else {
                        handShakeWatchDog(WATCHDOG_TIMER,"STEP_4_OBJECT_MESSAGE or FINISH_STEP_4");
                    }
                    break;

                case STEP_3_TEXT_MESSAGES:
                    step=11;//11
                    Log.e(TAG, "STEP_3_TEXT_MESSAGES. Initiator: "+mInitiator );
                    // reset watch dog on this step;
                    watchDogHandler.removeCallbacks(stepWatchDogRunnable);

                    ArrayList<RelayMessage> relayMessages =
                            JsonConvertor.getRelayMessageListFromJsonContent(jsonPacket);
                    timePerformance.start();
                    step=12;//12
                    for (RelayMessage relayMessage : relayMessages) {
                        updateReceivedMessage(relayMessage);
                        mDataManager.getMessagesDB().addMessage(relayMessage);
                        // alert device when he gets new message
                        UUID destId = relayMessage.getDestinationId();
                        step=13;//13
                        // check content in case its a message that I received again after i recover my user
                        if (destId.equals(mMyNode.getId()) && !relayMessage.getContent().equals("")) {
                            step=14;//14
                            String msg = "New message "+DELIMITER+relayMessage.getContent();
                            sendMessageToManager(NEW_RELAY_MESSAGE, msg);
                        }
                    }
                    Log.e(TAG, "TIME TO : " + "updateReceivedMessage" + " " + timePerformance.stop());

                    if (!mInitiator) {
                        step=15;//15
                        sendPacket(STEP_3_TEXT_MESSAGES, JsonConvertor.convertToJson(textMessagesToSend));
                    } else {
                        timePerformance.start();
                        step=16;//16
                        updateSentTextMessages();
                        Log.e(TAG, "TIME TO : " + "updateSentTextMessages" + " " + timePerformance.stop());
                        timePerformance.start();
                        // send first attachment if exist
                        if (objectMessagesToSend.size() > 0) {
                            step=17;//17
                            Log.e(TAG, "there are  "+objectMessagesToSend.size()+ " objects to send, sending " +attachmentsCounter +" from "+objectMessagesToSend.size() );
                            sendPacket(STEP_4_OBJECT_MESSAGE, JsonConvertor.convertToJson(objectMessagesToSend.get(attachmentsCounter)));
                            attachmentsCounter ++;
                        } else{
                            step=18;//18
                            Log.e(TAG, "there are  "+objectMessagesToSend.size()+ " objects to send, sending " +attachmentsCounter +" from "+objectMessagesToSend.size() );
                            Log.e(TAG, "TIME TO : " + "objectMessagesToSend" + " " + timePerformance.stop());
                            sendPacket(FINISH_STEP_4, new String("DUMMY"));
                        }
                    }
                    // start watchdog in this step
                    if (!mInitiator) {
                        handShakeWatchDog(WATCHDOG_TIMER,"STEP_3_TEXT_MESSAGES");
                    } else {
                        handShakeWatchDog(WATCHDOG_TIMER,"STEP_4_OBJECT_MESSAGE or FINISH_STEP_4");
                    }
                    break;


                case ACK_OBJECT_STEP_4:
                    step=11;//11
                    Log.e(TAG, "ACK_OBJECT_STEP_4: "+mInitiator );
                    // reset watch dog on this step;
                    watchDogHandler.removeCallbacks(stepWatchDogRunnable);
                    //while there is attachments, send them one by one
                    if (attachmentsCounter < objectMessagesToSend.size()) {

                        step=19;//19
                        Log.e(TAG, "there are  "+objectMessagesToSend.size()+ " objects to send, sending " +attachmentsCounter +" from "+objectMessagesToSend.size() );
                        sendPacket(STEP_4_OBJECT_MESSAGE, JsonConvertor.convertToJson(objectMessagesToSend.get(attachmentsCounter)));
                        attachmentsCounter ++;
                    }
                    else{
                        if (!mInitiator) {
                            step=20;//20
                            sendPacket(FINISH, new String("DUMMY"));
                            updateSentAttachmentMessages();
                            finishHandshake();
                        }
                        else{
                            step=21;//21
                            Log.e(TAG, "TIME TO : " + "objectMessagesToSend" + " " + timePerformance.stop());
                            sendPacket(FINISH_STEP_4, new String("DUMMY"));
                        }
                    }
                    // start watchdog in this step
                    if (!mInitiator) {
                        handShakeWatchDog(WATCHDOG_TIMER,"FINISH");
                    } else {
                        handShakeWatchDog(WATCHDOG_TIMER,"FINISH_STEP_4");
                    }
                    break;

                case STEP_4_OBJECT_MESSAGE:
                    step=22;//22
                    Log.e(TAG, "STEP_4_OBJECT_MESSAGE. Initiator: "+mInitiator );
                    // reset watch dog on this step;
                    watchDogHandler.removeCallbacks(stepWatchDogRunnable);
                    RelayMessage relayMessage = JsonConvertor.getRelayMessageFromJsonContent(jsonPacket);
                    updateReceivedMessage(relayMessage);
                    mDataManager.getMessagesDB().addMessage(relayMessage);
                    // alert device when he gets new message
                    UUID destId = relayMessage.getDestinationId();
                    step=13;//13
                    if (destId.equals(mMyNode.getId())) {
                        step=14;//14
                        String msg = "New message"+DELIMITER+"Received image";
                        sendMessageToManager(NEW_RELAY_MESSAGE, msg);
                    }
                    Log.e(TAG, "ACK object message");
                    sendPacket(ACK_OBJECT_STEP_4, new String("DUMMY"));
                    // start watchdog in this step
                    if (!mInitiator) {
                        handShakeWatchDog(WATCHDOG_TIMER,"ACK_OBJECT_STEP_4");
                    } else {
                        handShakeWatchDog(WATCHDOG_TIMER,"ACK_OBJECT_STEP_4");
                    }
                    break;

                case FINISH_STEP_4: // only !mInitiator get it
                    Log.e(TAG, "FINISH_STEP_4. Initiator: "+mInitiator );
                    // reset watch dog on this step;
                    watchDogHandler.removeCallbacks(stepWatchDogRunnable);
                    updateSentTextMessages();
                    if (!mInitiator) {
                        step=23;//23
                        // send first attachment if exist
                        if (objectMessagesToSend.size() > 0) {
                            step=24;//24
                            sendPacket(STEP_4_OBJECT_MESSAGE, JsonConvertor.convertToJson(objectMessagesToSend.get(attachmentsCounter)));
                            attachmentsCounter ++;
                        }
                        else{
                            step=25;//25
                            // update BluetoothConnected finish properly so the close thread will be without exception
                            mBluetoothConnected.finishSyncProperly();
                            sendPacket(FINISH, new String("DUMMY"));
                            updateSentAttachmentMessages();
                            // wait the finish will get to the sync device and than finish
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finishHandshake();
                                }
                            },200);

                        }
                    }
                    break;

                case FINISH:
                    step=26;//26
                    Log.e(TAG, "FINISH. Initiator: "+mInitiator );
                    // update BluetoothConnected finish properly so the close thread will be without exception
                    mBluetoothConnected.finishSyncProperly();
                    // reset watch dog on this step;
                    watchDogHandler.removeCallbacks(stepWatchDogRunnable);
                    updateSentAttachmentMessages();
                    finishHandshake();
                    break;
            }
        } catch (Exception e) {
            // reset watch dog on this step;
            watchDogHandler.removeCallbacks(stepWatchDogRunnable);
            mBluetoothConnected.cancel();
            Log.e(TAG,"Error in hand shake method,error-"+e.getMessage()+", step- "+step);
            sendMessageToManager(FAILED_DURING_HAND_SHAKE,"Error in hand shake method,error-"+e.getMessage()+", step- "+step);
        }
    }

    /**
     * if msg status is created, update it to sent
     * if the msg destination is me update msg status to delivered
     * if the msg status is delivered and i'm not the destination or the sender delete the content (double check)
     * @param msg
     */
    private void updateReceivedMessage(RelayMessage msg){

        if (msg.getStatus() == RelayMessage.STATUS_MESSAGE_CREATED)
            msg.setStatus(RelayMessage.STATUS_MESSAGE_SENT);
        if (msg.getDestinationId().equals(mMyNode.getId()))
            msg.setStatus(RelayMessage.STATUS_MESSAGE_DELIVERED);
        if (msg.getStatus() == RelayMessage.STATUS_MESSAGE_DELIVERED){
            // the msg already supposed to be with empty content. double check(delete in the future)
            if ( !msg.getDestinationId().equals(mMyNode.getId()) &&
                    !msg.getSenderId().equals(mMyNode.getId())){
                msg.deleteContent();
                msg.deleteAttachment();
            }
        }
    }

    /**
     * If I get here, it's means that all my text messages where delivered therefor:
     * if text msg status is created and  the sender id or destination id is in device metadata or
     * in the new node the the device received, change the status to sent.
     * if the msg destination is the device update msg status to delivered and delete
     * the content (unless i sent the msg)/
     */
    private void updateSentTextMessages(){

        for (RelayMessage message : textMessagesToSend){
            // because the messages in the textMessagesToSend were changed to delivered, I cant use them.
            RelayMessage msg = mDataManager.getMessagesDB().getMessage(message.getId());

            UUID destinationId = msg.getDestinationId();
            UUID senderId = msg.getSenderId();

            if ( msg.getStatus() == RelayMessage.STATUS_MESSAGE_CREATED){
                if (receivedKnownRelations.containsKey(destinationId) ||
                        receivedKnownRelations.containsKey(senderId)||
                        newNodeIdList.containsKey(destinationId) ||
                        newNodeIdList.containsKey(senderId)){
                    msg.setStatus(RelayMessage.STATUS_MESSAGE_SENT);
                }
            }

            if (receivedMetadata.getMyNode().getId().equals(destinationId)) {
                msg.setStatus(RelayMessage.STATUS_MESSAGE_DELIVERED);
                if (!mMyNode.getId().equals(msg.getSenderId()))
                    msg.deleteContent();
            }
            // update msg
            mDataManager.getMessagesDB().addMessage(msg);
        }
    }

    private void updateSentAttachmentMessages(){

        for (RelayMessage message : objectMessagesToSend){

            // because the messages in the textMessagesToSend were changed to delivered, I cant use them.
            RelayMessage msg = mDataManager.getMessagesDB().getMessage(message.getId());

            UUID destinationId = msg.getDestinationId();
            UUID senderId = msg.getSenderId();

            if ( msg.getStatus() == RelayMessage.STATUS_MESSAGE_CREATED){
                if (receivedKnownRelations.containsKey(destinationId) ||
                        receivedKnownRelations.containsKey(senderId)||
                        newNodeIdList.containsKey(destinationId) ||
                        newNodeIdList.containsKey(senderId)){
                    msg.setStatus(RelayMessage.STATUS_MESSAGE_SENT);
                }
            }
            if (receivedMetadata.getMyNode().getId().equals(destinationId)) {
                msg.setStatus(RelayMessage.STATUS_MESSAGE_DELIVERED);
                if (!mMyNode.getId().equals(msg.getSenderId()))
                    msg.deleteAttachment();
            }
            // update msg
            mDataManager.getMessagesDB().addMessage(msg);
        }
    }

    /**
     * Update all messages status and create messagesList to send
     * message to send : if the device doesn't have the msg and the sender id or the
     * destination id is in device's graph or in the new nodes that the device is going to get.
     * make sure that if it's my msg that already delivered,add the msg without content.
     * update messages:
     * go over all the received msgs , if the msg status is higher, update it. if the
     * status is delivered , delete the content(only if i'm not the destination or sender)
     * @return
     */
    private void updateMessagesAndCreateMessagesListToSend(){

        ArrayList<UUID> messageIdList = mDataManager.getMessagesDB().getMessagesIdList();
        for (UUID uuid : messageIdList){
            // new message
            if (!receivedKnownMessage.containsKey(uuid)) {
                RelayMessage relayMessage = mDataManager.getMessagesDB().getMessage(uuid);
                UUID destinationId = relayMessage.getDestinationId();
                UUID senderId = relayMessage.getSenderId();

                // if the sender or the destination in the receivedKnownRelations(include all the degrees) or the of the device, add to message list
                if (receivedKnownRelations.containsKey(destinationId) ||
                        receivedKnownRelations.containsKey(senderId)||
                        newNodeIdList.containsKey(destinationId) ||
                        newNodeIdList.containsKey(senderId)) {

                    // if the sender of the msg is the sync device but he doesn't have the msg,
                    // it's means that he deleted it, therefor don't pass the msg and update it as
                    // a delivered msg
                    if (senderId.equals(receivedMetadata.getMyNode().getId()))
                        relayMessage.setStatus(RelayMessage.STATUS_MESSAGE_DELIVERED);

                    // if the message status is already delivered, delete the content of the message and
                    // send only the 'log' of the message
                    if (relayMessage.getStatus() == RelayMessage.STATUS_MESSAGE_DELIVERED){
                        relayMessage.deleteAttachment();
                        relayMessage.deleteContent();
                    }

                    // add message according to its type
                    if (relayMessage.getType() == RelayMessage.TYPE_MESSAGE_TEXT)
                        textMessagesToSend.add(relayMessage);
                    else{
                        // if the message type is object message but the attachment is's null because
                        // the message status is delivered, add the message to text message list to make the hand shake
                        // be quicker.
                        if (relayMessage.getAttachment() == null)
                            textMessagesToSend.add(relayMessage);
                        else
                            objectMessagesToSend.add(relayMessage);
                    }

                }
            }
            else{
                // my device recognize this msg
                // update message status if needed
                int status = receivedKnownMessage.get(uuid).getStatus();
                RelayMessage msg = mDataManager.getMessagesDB().getMessage(uuid);
                if ( status > msg.getStatus() ){
                    msg.setStatus(status);
                    if (status == RelayMessage.STATUS_MESSAGE_DELIVERED){
                        if ( !msg.getSenderId().equals(mMyNode.getId()) &&
                                !msg.getDestinationId().equals(mMyNode.getId())){
                            msg.deleteAttachment();
                            msg.deleteContent();
                        }
                    }
                    // update msg status and content
                    mDataManager.getMessagesDB().addMessage(msg);
                }
            }
        }
    }

    private boolean updateNodeAndRelations
            (DataTransferred.UpdateNodeAndRelations updateNodeAndRelations){
        ArrayList<Node> nodeArrayList = updateNodeAndRelations.getNodeList();
        ArrayList<DataTransferred.NodeRelations> nodeRelationsArrayList =
                updateNodeAndRelations.getRelationsList();

        Log.e(TAG,"updateNodeAndRelations: node to update- "+nodeArrayList.size()+
                "\n relations to update- "+nodeRelationsArrayList.size());

        for ( Node node : nodeArrayList){
            // update the new node without timStamp
            mDataManager.getNodesDB().addNode(node);
        }

        for (DataTransferred.NodeRelations nodeRelations : nodeRelationsArrayList){
            ArrayList<UUID> uuidArrayList = nodeRelations.getRelations();
            for (UUID uuid : uuidArrayList){
                mDataManager.getGraphRelations().addEdge(nodeRelations.getNodeId(),uuid);
            }
        }
        return true;
    }

    /**
     * createUpdateNodeAndRelations
     * in this process newNodeIdList will be updated. it's not connected to UpdateNodeAndRelations
     * @param degree
     * @return
     */
    private DataTransferred.UpdateNodeAndRelations createUpdateNodeAndRelations(int degree){


        newNodeIdList = new HashMap<>();
        ArrayList<Node> updateNodeList = new ArrayList<>();
        ArrayList<DataTransferred.NodeRelations> updateRelationsList = new ArrayList<>();

        try {
            ArrayList<UUID> myNodeList = mDataManager.getNodesDB().getNodesIdList();


            for (UUID nodeId : myNodeList) {

                DataTransferred.NodeRelations tempRelations =
                        mDataTransferred.createNodeRelations(
                                nodeId,
                                mDataManager.getNodesDB().getNode(nodeId).getTimeStampNodeDetails(),
                                mDataManager.getGraphRelations().adjacentTo(nodeId));

                // if node is known check if need to update its node and/or relations and it's not the syncing node
//                if (receivedKnownRelations.containsKey(nodeId) &&
//                        !nodeId.equals(receivedMetadata.getMyNode().getId())) {
                if (receivedKnownRelations.containsKey(nodeId)) {
                    // if my node timestamp is newer ' update node and relation
                    if (mDataManager.getNodesDB().getNode(nodeId).getTimeStampNodeDetails().after(
                            receivedKnownRelations.get(nodeId).getTimeStampNodeDetails())) {

                        updateNodeList.add(mDataManager.getNodesDB().getNode(nodeId));
                        newNodeIdList.put(nodeId, mDataManager.getNodesDB().getNode(nodeId));
                    }

                    if (mDataManager.getNodesDB().getNode(nodeId).getTimeStampNodeRelations().after(
                            receivedKnownRelations.get(nodeId).getTimeStampNodeRelations())) {

                        // if timestamp relation is newer, update relation
                        updateRelationsList.add(tempRelations);
                        newNodeIdList.put(nodeId, mDataManager.getNodesDB().getNode(nodeId));
                    }
                }
                // if node is in the share final degree and it's not the syncing node, add it to update nodes and his relations
                else {
                    if(knownRelations.get(nodeId) != null)
//                        if (knownRelations.get(nodeId).getNodeDegree() <= degree &&
//                                !nodeId.equals(receivedMetadata.getMyNode().getId())) {
                        if (knownRelations.get(nodeId).getNodeDegree() <= degree) {
                            updateNodeList.add(mDataManager.getNodesDB().getNode(nodeId));
                            updateRelationsList.add(tempRelations);
                            newNodeIdList.put(nodeId, mDataManager.getNodesDB().getNode(nodeId));
                        }
                }
            }
            updateNodeAndRelations = mDataTransferred.createUpdateNodeAndRelations(updateNodeList,
                    updateRelationsList);
            Log.e(TAG,"createUpdateNodeAndRelations:\n node sent to update: "+updateNodeList.size()+
                    "\n relations to update: "+updateRelationsList.size());
            return updateNodeAndRelations;
        }catch(Exception e){

            Log.e(TAG,"something wrong here- "+e.getMessage());
        }
        return updateNodeAndRelations;
    }

    /**
     *  Calculate which rank is higher Node rank or HandShake Rank
     */
    private int CalculateFinalRank(){

        int handShakeNode = mDataManager.getMyHandShakeHistoryRankWith(mMyNode.getId());
        if (handShakeNode > mMyNode.getRank() )
            return handShakeNode;
        else
            return mMyNode.getRank();

    }

    /**
     * The method will check if my node has an updated rank in the other device. if yes it will update
     * the node rank for the handshake. during the handshake all the node details will be update
     * @return
     */
    private boolean checkSyncNodeRankBeforeHandShake(){
        // check if the device had handshake with me knows me - if yes check if he has update rank for me
        if (receivedKnownRelations.containsKey(mMyNode.getId())){

            if (receivedKnownRelations.get(mMyNode.getId()).getTimeStampRankFromServer()
                    .after(mMyNode.getTimeStampRankFromServer()));
            // update my rank (need to use it) after it will update all node
            mMyNode.setRank(receivedKnownRelations.get(mMyNode.getId()).getNodeRank(),receivedKnownRelations.get(mMyNode.getId()).getTimeStampRankFromServer());
            return true;
        }
        return false;
    }

    private void sendPacket(int command,String jsonContent){
        String jsonPacket = JsonConvertor.createJsonWithCommand(command,jsonContent);
        JsonConvertor.isJSONValid(jsonPacket);
        mBluetoothConnected.writePacket(jsonPacket);
        Log.e(TAG,"JsonString sent size is : "+jsonContent.length());
    }


    /**
     * Finish handshake process
     */
    private void finishHandshake() {
        // On BLManager  there will be update messages status
        //  add event in history
        mDataManager.getHandShakeDB().addEventToHandShakeHistoryWith(receivedMetadata.getMyNode().getId(),mInitiator);
        //  add edge between node
        mDataManager.getGraphRelations().addEdge(mMyNode.getId(), receivedMetadata.getMyNode().getId());
        sendFinishMessageToManager(FINISHED_HANDSHAKE, mBluetoothSocket.getRemoteDevice().getAddress());
        watchDogHandler.removeCallbacksAndMessages(null);
        Log.d(TAG, "FINISHED_HANDSHAKE");


    }


    /**
     * Send message to the bluetoothManager class
     */
    private void sendFinishMessageToManager(int m, String address)  {

        // Send address as a String
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        bundle.putString("deviceUUID", receivedMetadata.getMyNode().getId().toString());
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendFinishMessageToManager ");
        }
    }


    /**
     * Send  relay message to the bluetoothManager class
     */
    private void sendMessageToManager(int m,String message)  {

        // Send address as a String
        Bundle bundle = new Bundle();
        bundle.putString("relayMessage", message);
        bundle.putString("message", message);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendFinishMessageToManager ");
        }
    }

    public void closeConnection(){

        mBluetoothConnected.cancel();
    }



}
