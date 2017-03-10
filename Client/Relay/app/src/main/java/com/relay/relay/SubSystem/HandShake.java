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
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
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

    // TEST
    private boolean testReceieved = false;
    //

    public HandShake(BluetoothSocket bluetoothSocket,
                     Messenger messenger, boolean initiator, Context context,DataManager dataManager){

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

        this.mBluetoothConnected.start();
        startHandshake();
    }

    /**
     * Start handshake process
     */
    private void startHandshake() {
        this.metadata = mDataTransferred.createMetaData();
        this.knownRelations = metadata.getKnownRelationsList();
        if(mInitiator){
            sendPacket(STEP_1_METADATA,JsonConvertor.convertToJson(metadata));
        }
    }

    /**
     * Getter of incoming messages from one of the BluetoothConnected
     */
    public void getPacket(String jsonPacket){

        Log.e(TAG,jsonPacket);
        Log.e(TAG,String.valueOf(jsonPacket.length()));

        int command = JsonConvertor.getCommand(jsonPacket);

        switch (command){
            case STEP_1_METADATA:
                // receive meta data
                receivedMetadata = JsonConvertor.getMetadataFromJsonContent(jsonPacket);
                receivedKnownMessage = receivedMetadata.getKnownMessagesList();
                receivedKnownRelations = receivedMetadata.getKnownRelationsList();
                checkRankBeforeHandShake(receivedMetadata);
                finalDegree = CalculateFinalRank();
                updateNodeAndRelations = createUpdateNodeAndRelations(finalDegree);
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
                updateNodeAndRelations(receivedUpdateNodeAndRelations);
                updateMessagesAndCreateMessagesListToSend();
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
                for (RelayMessage relayMessage : relayMessages){
                    mDataManager.getMessagesDB().addMessage(relayMessage);
                }
                if(!mInitiator){
                    sendPacket(STEP_3_TEXT_MESSAGES,JsonConvertor.convertToJson(textMessagesToSend));
                }
                else{
                    for(int i = 0; i < objectMessagesToSend.size();i++){
                        sendPacket(STEP_4_OBJECT_MESSAGE,JsonConvertor.convertToJson(objectMessagesToSend.get(i)));
                    }
                    sendPacket(FINISH_STEP_4,new String("DUMMY"));
                }
                break;

            case FINISH_STEP_4:
                if(!mInitiator){
                    for(int i = 0; i < objectMessagesToSend.size();i++){
                        sendPacket(STEP_4_OBJECT_MESSAGE,JsonConvertor.convertToJson(objectMessagesToSend.get(i)));
                    }
                    sendPacket(FINISH,new String("DUMMY"));
                    finishHandshake();
                }
                break;
            case STEP_4_OBJECT_MESSAGE:
                RelayMessage relayMessage = JsonConvertor.getRelayMessageFromJsonContent(jsonPacket);
                mDataManager.getMessagesDB().addMessage(relayMessage);
                break;
            case FINISH:
                finishHandshake();
                break;
        }
    }


    /**
     * update messages status from received known messages
     * add to textMessagesToSend and to objectMessageToSend
     * all the  text messages that relevant to the device
     * @return
     */
    private boolean updateMessagesAndCreateMessagesListToSend(){

        ArrayList<UUID> messageIdList = mDataManager.getMessagesDB().getMessagesIdList();

        for (UUID uuid : messageIdList){
            // new message
            if (!receivedKnownMessage.containsKey(uuid)) {
                RelayMessage relayMessage = mDataManager.getMessagesDB().getMessage(uuid);
                UUID destinationId = relayMessage.getDestinationId();
                UUID sender = relayMessage.getSenderId();
                // if the sender or the destination in the nodeList of device add to message list
                if (receivedKnownRelations.containsKey(destinationId) ||
                        receivedKnownRelations.containsKey(sender)) {
                    // add message according to its type
                    if ( relayMessage.getType() == RelayMessage.TYPE_MESSAGE_TEXT )
                        textMessagesToSend.add(relayMessage);
                    else
                        objectMessagesToSend.add(relayMessage);
                }
            }
            else{
                // update message status if needed
                int status = receivedKnownMessage.get(uuid).getStatus();
                if ( status > mDataManager.getMessagesDB().getMessage(uuid).getStatus() ){
                    mDataManager.getMessagesDB().getMessage(uuid).setStatus(status);
                }

            }
        }
        return true;
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

    private DataTransferred.UpdateNodeAndRelations createUpdateNodeAndRelations(int degree){

        ArrayList<Node> updateNodeList = new ArrayList<>();
        ArrayList<DataTransferred.NodeRelations> updateRelationsList = new ArrayList<>();
        ArrayList<UUID> myNodeList = mDataManager.getNodesDB().getNodesIdList();


        for (UUID nodeId : myNodeList){
            // if node is known check if need to update its node and/or relations
            if (receivedKnownRelations.containsKey(nodeId)){

                DataTransferred.NodeRelations tempRelations =
                         mDataTransferred.createNodeRelations(
                                nodeId,
                                mDataManager.getNodesDB().getNode(nodeId).getTimeStampNodeDetails(),
                                mDataManager.getGraphRelations().adjacentTo(nodeId));

                // if my node timestamp is newer ' update node and relation
                if (mDataManager.getNodesDB().getNode(nodeId).getTimeStampNodeDetails().after(
                        receivedKnownRelations.get(nodeId).getTimeStampNodeDetails())){

                    updateNodeList.add(mDataManager.getNodesDB().getNode(nodeId));
                    updateRelationsList.add(tempRelations);
                }else{
                    // if timestamp relation is newer, update relation
                    updateRelationsList.add(tempRelations);
                }
            }
            // if node is in the share final degree, add it to update nodes
            else{
                if (knownRelations.get(nodeId).getNodeDegree() <= finalDegree){
                    updateNodeList.add(mDataManager.getNodesDB().getNode(nodeId));
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
