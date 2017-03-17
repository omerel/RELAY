package com.relay.relay.SubSystem;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.relay.relay.Bluetooth.BLConstants;
import com.relay.relay.Bluetooth.BluetoothConnected;
import com.relay.relay.Util.DataTransferred;
import com.relay.relay.Util.JsonConvertor;
import com.relay.relay.Util.TimePerformence;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 12/12/2016.
 * HandShake using bluetooth connection
 */

public class HandShake implements BLConstants {


    private final String TAG = "RELAY_DEBUG: "+ HandShake.class.getSimpleName();
    // commands
    private final int STEP_1_METADATA = 1;
    private final int FINISH_STEP_1 = 2;
    private final int STEP_2_UPDATE_NODES_AND_RELATIONS = 3;
    private final int STEP_3_TEXT_MESSAGES = 5;
    private final int STEP_4_OBJECT_MESSAGE = 6;
    private final int FINISH_STEP_4 = 7;
    private final int FINISH = 8;

    private Messenger mMessenger;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothConnected mBluetoothConnected;
    private boolean mInitiator;
    private DataManager mDataManager;
    private DataTransferred mDataTransferred;
    private Node mMyNode;

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
    private TimePerformence timePerformence = new TimePerformence();



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
        this.mMyNode = mDataManager.getNodesDB().getNode(mDataManager.getNodesDB().getMyNodeId());
        this.textMessagesToSend = new ArrayList<>();
        this.objectMessagesToSend = new ArrayList<>();
        this.metadata = metadata;
        this.mBluetoothConnected.start();
        startHandshake();
    }

    /**
     * Start handshake process
     */
    private void startHandshake() {

        timePerformence.start();
        this.metadata = mDataTransferred.createMetaData();
        Log.e(TAG,"TIME TO : "+"createMetaData"+" "+timePerformence.stop());
        this.knownRelations = metadata.getKnownRelationsList();
        if(mInitiator){
            sendPacket(STEP_1_METADATA,JsonConvertor.convertToJson(metadata));
        }
    }

    /**
     * Getter of incoming messages from one of the BluetoothConnected
     */

    public void getPacket(String jsonPacket){
        int command = JsonConvertor.getCommand(jsonPacket);
        switch (command){
            case STEP_1_METADATA:
                // receive meta data
                receivedMetadata = JsonConvertor.getMetadataFromJsonContent(jsonPacket);
                receivedKnownMessage = receivedMetadata.getKnownMessagesList();
                receivedKnownRelations = receivedMetadata.getKnownRelationsList();
                timePerformence.start();
                checkRankBeforeHandShake(receivedMetadata);
                Log.e(TAG,"TIME TO : "+"checkRankBeforeHandShake"+" "+timePerformence.stop());
                finalDegree = CalculateFinalRank();
                timePerformence.start();
                updateNodeAndRelations = createUpdateNodeAndRelations(finalDegree);
                Log.e(TAG,"TIME TO : "+"createUpdateNodeAndRelations"+" "+timePerformence.stop());
                if(!mInitiator){
                    sendPacket(STEP_1_METADATA,JsonConvertor.convertToJson(metadata));
                }
                else{
                    sendPacket(STEP_2_UPDATE_NODES_AND_RELATIONS,JsonConvertor.convertToJson(updateNodeAndRelations));
                }
                break;
            case STEP_2_UPDATE_NODES_AND_RELATIONS:
                receivedUpdateNodeAndRelations =
                        JsonConvertor.getUpdateNodeAndRelationsFromJsonContent(jsonPacket);
                timePerformence.start();
                updateNodeAndRelations(receivedUpdateNodeAndRelations);
                Log.e(TAG,"TIME TO : "+"updateNodeAndRelations"+" "+timePerformence.stop());
                timePerformence.start();
                updateMessagesAndCreateMessagesListToSend();
                Log.e(TAG,"TIME TO : "+"updateMessagesAndCreateMessagesListToSend"+" "+timePerformence.stop());

                if(!mInitiator){
                    sendPacket(STEP_2_UPDATE_NODES_AND_RELATIONS,JsonConvertor.convertToJson(updateNodeAndRelations));
                }
                else{
                    sendPacket(STEP_3_TEXT_MESSAGES,JsonConvertor.convertToJson(textMessagesToSend));
                }
                break;
            case STEP_3_TEXT_MESSAGES:

                ArrayList<RelayMessage> relayMessages =
                         JsonConvertor.getRelayMessageListFromJsonContent(jsonPacket);
                timePerformence.start();
                for (RelayMessage relayMessage : relayMessages){
                    updateReceivedMessage(relayMessage);
                    mDataManager.getMessagesDB().addMessage(relayMessage);
                }
                Log.e(TAG,"TIME TO : "+"updateReceivedMessage"+" "+timePerformence.stop());

                if(!mInitiator){
                    sendPacket(STEP_3_TEXT_MESSAGES,JsonConvertor.convertToJson(textMessagesToSend));
                }
                else{
                    timePerformence.start();
                    updateSentTextMessages();
                    Log.e(TAG,"TIME TO : "+"updateSentTextMessages"+" "+timePerformence.stop());
                    timePerformence.start();
                    for(int i = 0; i < objectMessagesToSend.size();i++){
                        sendPacket(STEP_4_OBJECT_MESSAGE,JsonConvertor.convertToJson(objectMessagesToSend.get(i)));
                    }
                    Log.e(TAG,"TIME TO : "+"objectMessagesToSend"+" "+timePerformence.stop());
                    sendPacket(FINISH_STEP_4,new String("DUMMY"));
                }
                break;

            case STEP_4_OBJECT_MESSAGE:
                RelayMessage relayMessage = JsonConvertor.getRelayMessageFromJsonContent(jsonPacket);
                updateReceivedMessage(relayMessage);
                mDataManager.getMessagesDB().addMessage(relayMessage);
                break;

            case FINISH_STEP_4:
                updateSentTextMessages();
                if(!mInitiator){
                    for(int i = 0; i < objectMessagesToSend.size();i++){
                        sendPacket(STEP_4_OBJECT_MESSAGE,JsonConvertor.convertToJson(objectMessagesToSend.get(i)));
                    }
                    sendPacket(FINISH,new String("DUMMY"));
                    updateSentAttachmentMessages();
                    finishHandshake();
                }
                break;
            case FINISH:
                updateSentAttachmentMessages();
                finishHandshake();
                break;
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
             if ( !msg.getDestinationId().equals(mMyNode.getId()) &&
                     !msg.getSenderId().equals(mMyNode.getId())){
                 msg.deleteContent();
                 msg.deleteAttachments();
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

        for (RelayMessage msg : textMessagesToSend){

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

        for (RelayMessage msg : objectMessagesToSend){

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
                    msg.deleteAttachments();
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
                // if the sender or the destination in the nodeList of device add to message list

                if (receivedKnownRelations.containsKey(destinationId) ||
                        receivedKnownRelations.containsKey(senderId)||
                        newNodeIdList.containsKey(destinationId) ||
                        newNodeIdList.containsKey(senderId)) {
                    // add message according to its type
                    if ( relayMessage.getType() == RelayMessage.TYPE_MESSAGE_TEXT )
                        textMessagesToSend.add(relayMessage);
                    else
                        objectMessagesToSend.add(relayMessage);
                }
            }
            else{// my device recognise this msg
                // update message status if needed
                int status = receivedKnownMessage.get(uuid).getStatus();
                RelayMessage msg = mDataManager.getMessagesDB().getMessage(uuid);
                if ( status > msg.getStatus() ){
                    msg.setStatus(status);
                    if (status == RelayMessage.STATUS_MESSAGE_DELIVERED){
                        if ( !msg.getSenderId().equals(mMyNode.getId()) &&
                                !msg.getDestinationId().equals(mMyNode.getId())){
                            msg.deleteAttachments();
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

        for ( Node node : nodeArrayList){
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
        ArrayList<UUID> myNodeList = mDataManager.getNodesDB().getNodesIdList();


        for (UUID nodeId : myNodeList){

            DataTransferred.NodeRelations tempRelations =
                    mDataTransferred.createNodeRelations(
                            nodeId,
                            mDataManager.getNodesDB().getNode(nodeId).getTimeStampNodeDetails(),
                            mDataManager.getGraphRelations().adjacentTo(nodeId));

            // if node is known check if need to update its node and/or relations
            if (receivedKnownRelations.containsKey(nodeId)){

                // if my node timestamp is newer ' update node and relation
                if (mDataManager.getNodesDB().getNode(nodeId).getTimeStampNodeDetails().after(
                        receivedKnownRelations.get(nodeId).getTimeStampNodeDetails())){

                    updateNodeList.add(mDataManager.getNodesDB().getNode(nodeId));
                    updateRelationsList.add(tempRelations);
                    newNodeIdList.put(nodeId,mDataManager.getNodesDB().getNode(nodeId));
                }else{
                    // if timestamp relation is newer, update relation
                    updateRelationsList.add(tempRelations);
                }
            }
            // if node is in the share final degree, add it to update nodes and his relations
            else{
                if (knownRelations.get(nodeId).getNodeDegree() <= degree){
                    updateNodeList.add(mDataManager.getNodesDB().getNode(nodeId));
                    updateRelationsList.add(tempRelations);
                    newNodeIdList.put(nodeId,mDataManager.getNodesDB().getNode(nodeId));
                }
            }
        }
        updateNodeAndRelations = mDataTransferred.createUpdateNodeAndRelations(updateNodeList,
                updateRelationsList );
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
     * @param metadata
     * @return
     */
    private boolean checkRankBeforeHandShake(DataTransferred.Metadata metadata){
        // check if the device had handshake with me knows me - if yes check if he has update rank for me

        if (receivedKnownRelations.containsKey(mMyNode.getId())){
            if (receivedKnownRelations.get(mMyNode.getId()).getTimeStampNodeDetails()
                    .after(mMyNode.getTimeStampNodeDetails()));
                // update my rank (need to use it) after it will update all node
                mMyNode.setRank(receivedKnownRelations.get(mMyNode.getId()).getNodeRank());
            return true;
        }
        return false;
    }

    private void sendPacket(int command,String jsonContent){
        String jsonPacket = JsonConvertor.createJsonWithCommand(command,jsonContent);
        mBluetoothConnected.writePacket(jsonPacket);
    }


    /**
     * Finish handshake process
     */
    private void finishHandshake() {
        // On BLManager  there will be update messages status
        //  add event in history
        mDataManager.getHandShakeDB().addEventToHandShakeHistoryWith(receivedMetadata.getMyNode().getId());
        //  add edge between node
        mDataManager.getGraphRelations().addEdge(mMyNode.getId(),
                receivedMetadata.getMyNode().getId());
        // TODO change
        // send finish handshake to Log
        sendRelayMessageToManager(NEW_RELAY_MESSAGE,"finish handshake with:\n"+
                mBluetoothSocket.getRemoteDevice().getAddress());
        // Send message back to the bluetooth manager - bluetooth address
        sendMessageToManager(FINISHED_HANDSHAKE, mBluetoothSocket.getRemoteDevice().getAddress());
        Log.d(TAG, "FINISHED_HANDSHAKE");

    }


    /**
     * Send message to the bluetoothManager class
     */
    private void sendMessageToManager(int m,String address)  {

        // Send address as a String
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        bundle.putString("deviceUUID", receivedMetadata.getMyNode().getId().toString());
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendMessageToManager ");
        }
    }


    /**
     * Send  relay message to the bluetoothManager class
     */
    private void sendRelayMessageToManager(int m,String relayMessage)  {

        // Send address as a String
        Bundle bundle = new Bundle();
        bundle.putString("relayMessage", relayMessage);
        Message msg = Message.obtain(null, m);
        msg.setData(bundle);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Problem with sendRelayMessageToManager ");
        }
    }

    public void closeConnection(){
        mBluetoothConnected.cancel();
    }
}
