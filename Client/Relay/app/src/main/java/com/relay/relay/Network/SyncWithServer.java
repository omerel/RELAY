package com.relay.relay.Network;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.Util.DataTransferred;
import com.relay.relay.Util.JsonConvertor;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 05/06/2017.
 */

public class SyncWithServer implements NetworkConstants{

    private final String TAG = "RELAY_DEBUG: "+ SyncWithServer.class.getSimpleName();

    // messenger to NetworkManager
    private Messenger mMessenger;
    private RelayConnectivityManager mRelayConnectivityManager;
    private DataManager mDataManager;
    // Instantiate the RequestQueue.
    private RequestQueue mQueue;
    private StringRequest mStringRequest;
    private Response.Listener<String> mListener;
    private Response.ErrorListener mErrorListener;
    private DataTransferred mDataTransferred;
    private int mStatus;
    private String mJson;
    private int mStep;

    private DataTransferred.Metadata metadata;
    private  DataTransferred.Metadata receivedMetadata;
    private Map<UUID,DataTransferred.KnownMessage> receivedKnownMessage;
    private ArrayList<RelayMessage> updateMessages;
    private DataTransferred.UpdateNodeAndRelations updateNodeAndRelations;
    private Map<UUID,DataTransferred.KnownRelations> knownRelations;
    private Map<UUID,DataTransferred.KnownRelations> receivedKnownRelations;
    private DataTransferred.UpdateNodeAndRelations receivedUpdateNodeAndRelations;
    private Map<UUID,Node> newNodeIdList; // store all the  new nodes that sent to the device. used to update messages status


    public SyncWithServer(Messenger messenger, RelayConnectivityManager relayConnectivityManager, DataManager dataManager) {

        this.mStep = -1;
        this.mMessenger = messenger;
        this.mDataManager = dataManager;
        this.mRelayConnectivityManager = relayConnectivityManager;
        this.mQueue = Volley.newRequestQueue(mRelayConnectivityManager);
        this.mDataTransferred = new DataTransferred(mDataManager.getGraphRelations(),
                mDataManager.getNodesDB(),mDataManager.getMessagesDB());

        // Listener for response from server
        this.mListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                mJson = response;
                switch (mStatus){
                    // test
                    case -1:
                        startSync();
                        Log.e(TAG,"THE RESPONSE IS :"+response);
                        sendMessageToManager(PROGRESS,"this is step test");
                        stopSync();
                        break;
                    //received metadata from server
                    case STEP_1_META_DATA:
                        sendMessageToManager(PROGRESS," received metadata from server");
                        receivedMetadata = JsonConvertor.getMetadataFromJsonContent(mJson);
                        receivedKnownMessage = receivedMetadata.getKnownMessagesList();
                        receivedKnownRelations = receivedMetadata.getKnownRelationsList();
                        updateMessages = updateMessagesAndCreateMessagesListToSend();
                        updateNodeAndRelations = createUpdateNodeAndRelations();
                        mStep++;
                        goToSyncStep(mStep);
                        break;
                    case STEP_2_BODY:
                        HashMap<String,String> body = (HashMap<String, String>) JsonConvertor.getJsonBody(mJson);
                        sendMessageToManager(PROGRESS," received body from server");
                        // update node and relations
                        receivedUpdateNodeAndRelations = JsonConvertor.getUpdateNodeAndRelationsFromJsonContent(body.get("updateNodeAndRelations"));
                        updateNodeAndRelations(receivedUpdateNodeAndRelations);

                        // update messages
                        ArrayList<RelayMessage> relayMessages = JsonConvertor.getRelayMessageListFromJsonContent(body.get("updateMessages"));
                        for (RelayMessage relayMessage : relayMessages) {
                            updateReceivedMessage(relayMessage);
                            mDataManager.getMessagesDB().addMessage(relayMessage);
                            // alert device when it gets new message
                            UUID destId = relayMessage.getDestinationId();
                            // check content in case its a message that I received again after i recover my user
                            if (destId.equals(mDataManager.getMyUuid()) && !relayMessage.getContent().equals("")) {
                                String msg = "@"+receivedMetadata.getMyNode().getUserName()+DELIMITER+relayMessage.getContent();
                                sendMessageToManager(NEW_RELAY_MESSAGE, msg);
                            }
                        }
                        // finish sync with server
                        sendMessageToManager(PROGRESS," finish successfully");
                        stopSync();
                        break;
                }
            }
        };
        // Listener for error response from server
        this.mErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                sendMessageToManager(ERROR_WHILE_SYNC,""+error);
                stopSync();
                Log.e(TAG,"THE ERROR IS :"+error);
            }
        };

        // start Sync with server
        test();

        // goToSyncStep(0);
    }



    private void startSync(){
        mStatus = SYNC_WITH_SERVER;
        sendMessageToManager(START_SYNC,"");

    }

    private void stopSync(){
        mStatus = NOT_SYNC_WITH_SERVER;
        sendMessageToManager(FINISH_SYNC,"");
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
        if (msg.getDestinationId().equals(mDataManager.getMyUuid()))
            msg.setStatus(RelayMessage.STATUS_MESSAGE_DELIVERED);
        if (msg.getStatus() == RelayMessage.STATUS_MESSAGE_DELIVERED){
            // the msg already supposed to be with empty content. double check(delete in the future)
            if ( !msg.getDestinationId().equals(mDataManager.getMyUuid()) &&
                    !msg.getSenderId().equals(mDataManager.getMyUuid())){
                msg.deleteContent();
                msg.deleteAttachment();
            }
        }
    }

    /**
     * createUpdateNodeAndRelations
     * In this process newNodeIdList will be updated. it's not connected to UpdateNodeAndRelations
     * @return
     */
    private DataTransferred.UpdateNodeAndRelations createUpdateNodeAndRelations(){

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
                // add it to update nodes and relations
                else {
                    updateNodeList.add(mDataManager.getNodesDB().getNode(nodeId));
                    updateRelationsList.add(tempRelations);
                    newNodeIdList.put(nodeId, mDataManager.getNodesDB().getNode(nodeId));
                }
            }
            updateNodeAndRelations = mDataTransferred.createUpdateNodeAndRelations(updateNodeList,
                    updateRelationsList);
            Log.e(TAG,"createUpdateNodeAndRelations:\n node sent to update: "+updateNodeList.size()+
                    "\n relations to update: "+updateRelationsList.size());
        }catch(Exception e){
            Log.e(TAG,"something wrong here- "+e.getMessage());
            mStatus = NOT_SYNC_WITH_SERVER;
        }
        return updateNodeAndRelations;
    }

    private boolean updateNodeAndRelations(DataTransferred.UpdateNodeAndRelations updateNodeAndRelations){
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

    private ArrayList<RelayMessage> updateMessagesAndCreateMessagesListToSend() {

        ArrayList<RelayMessage> messages = new ArrayList<>();
        ArrayList<UUID> messageIdList = mDataManager.getMessagesDB().getMessagesIdList();
        for (UUID uuid : messageIdList){
            // new message
            if (!receivedKnownMessage.containsKey(uuid)) {
                RelayMessage relayMessage = mDataManager.getMessagesDB().getMessage(uuid);
                UUID destinationId = relayMessage.getDestinationId();
                UUID senderId = relayMessage.getSenderId();

                // todo assume that the server sends all the relevant messages id's after calculate the new device network
                // if the message status is already delivered, delete the content of the message and
                // send only the 'log' of the message
                if (relayMessage.getStatus() == RelayMessage.STATUS_MESSAGE_DELIVERED){
                    relayMessage.deleteAttachment();
                    relayMessage.deleteContent();
                }
                messages.add(relayMessage);
            }
            else{
                // my device recognize this msg
                // update message status if needed
                int status = receivedKnownMessage.get(uuid).getStatus();
                RelayMessage msg = mDataManager.getMessagesDB().getMessage(uuid);
                if ( status > msg.getStatus() ){
                    msg.setStatus(status);
                    if (status == RelayMessage.STATUS_MESSAGE_DELIVERED){
                        if ( !msg.getSenderId().equals(mDataManager.getMyUuid()) &&
                                !msg.getDestinationId().equals(mDataManager.getMyUuid())){
                            msg.deleteAttachment();
                            msg.deleteContent();
                        }
                    }
                    // update msg status and content
                    mDataManager.getMessagesDB().addMessage(msg);
                }
            }
        }
        return messages;
    }


    private void goToSyncStep(int step){
        Map<String, String>  params;
        switch (step){
            // start sync, send meta data
            case STEP_1_META_DATA:
                params = new HashMap<String, String>();

                // create metadata
                metadata = mDataTransferred.createMetaData();
                params.put("metadata",JsonConvertor.convertToJson(metadata));

                // request metadata from server
                mStringRequest = new StringRequest(RELAY_URL+API_SYNC_METADATA, mListener, mErrorListener, params);
                mQueue.add(mStringRequest);
                break;

            // create content and send to server
            case STEP_2_BODY:
                params = new HashMap<String, String>();

                params.put("updateNodeAndRelations",JsonConvertor.convertToJson(updateNodeAndRelations));
                params.put("updateMessages",JsonConvertor.convertToJson(updateMessages));

                // request data
                mStringRequest = new StringRequest(RELAY_URL+API_SYNC_BODY,mListener,mErrorListener,params);
                mQueue.add(mStringRequest);
                break;
        }
    }
    private void test(){
        String s = "{ mId: '592e5be1038c0baa59bae8d7', mTimeStampRankFromServer: '2017-05-31T06:00:01.233Z', mFullName: 'The Omer', mUserName: 'Omer', mPhoneNumber: '050-5050505', mEmail: 'a@relay.com', mRank: '2' }";

        Map<String, String> par = new HashMap<String, String>();
        par.put("node",s);
        mStringRequest = new StringRequest(RELAY_URL+NODE+"592e5be1038c0baa59bae8d7",mListener,mErrorListener,par);
        mQueue.add(mStringRequest);
        Log.e(TAG, "create  mStringRequest");
    }


    public int getStatus(){return mStatus;}

    /**
     * Send  relay message to the NetworkManager
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
            Log.e(TAG, "Problem with sendMessageToManager ");
        }
    }
}
