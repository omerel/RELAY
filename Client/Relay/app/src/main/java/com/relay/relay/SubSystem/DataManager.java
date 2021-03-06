package com.relay.relay.SubSystem;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.relay.relay.DB.GraphRelations;
import com.relay.relay.DB.HandShakeDB;
import com.relay.relay.DB.InboxDB;
import com.relay.relay.DB.MessagesDB;
import com.relay.relay.DB.NodesDB;
import com.relay.relay.MainActivity;
import com.relay.relay.system.HandShakeHistory;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static com.relay.relay.DB.InboxDB.DELETE_MESSAGE_CONTENT_FROM_MESSAGE_DB;
import static com.relay.relay.SignInActivity.CURRENT_UUID_USER;

/**
 * Created by omer on 05/03/2017.
 * Data manager responsible for maintenance of the saved data.
 */

public class DataManager {

    private final String TAG = "RELAY_DEBUG: "+ DataManager.class.getSimpleName();


    private GraphRelations mGraphRelations;
    private MessagesDB mMessagesDB;
    private NodesDB mNodesDB;
    private HandShakeDB mHandShakeDB;
    private InboxDB mInboxDB;
    private Context context;
    private UUID myUuid;
    // Handler for incoming messages from inboxDB
    private final Messenger mMessengerFromDB = new Messenger(new IncomingHandler());

    public DataManager(Context context){

        this.context = context;
        this.mInboxDB =  new InboxDB(context,mMessengerFromDB);
        this.mGraphRelations = new GraphRelations(context);
        this.mNodesDB = new NodesDB(context,mGraphRelations,mInboxDB);
        this.mMessagesDB = new MessagesDB(context,mInboxDB);
        this.mHandShakeDB = new HandShakeDB(context);

        SharedPreferences sharedPreferences = context.getSharedPreferences(MainActivity.SYSTEM_SETTING,0);
        String tempuuid = sharedPreferences.getString(CURRENT_UUID_USER,"");

        if (tempuuid != "" ){
            myUuid = UUID.fromString(tempuuid);
            try {
                this.mNodesDB.setMyNodeId(myUuid);
            }
            catch ( Exception e){
                Log.e(TAG, "Error!, uuid not found");
            }
            this.mGraphRelations.setNodesDB(mNodesDB);
            this.mInboxDB.getMyNodeIdFromNodesDB(mNodesDB);
        }
        else{
            myUuid = null;
        }
    }

    public boolean isDataManagerSetUp(){
        return (myUuid != null);
    }

    public UUID getMyUuid(){return myUuid; }

    public GraphRelations getGraphRelations() {
        return mGraphRelations;
    }

    public MessagesDB getMessagesDB() {
        return mMessagesDB;
    }

    public NodesDB getNodesDB() {
        return mNodesDB;
    }

    public HandShakeDB getHandShakeDB() {
        return mHandShakeDB;
    }

    public InboxDB getInboxDB(){return mInboxDB;}


    public boolean deleteAllDataManager(){

        mNodesDB.deleteNodedb();
        mInboxDB.deleteDB();
        mHandShakeDB.deleteHandShakeDB();
        mGraphRelations.deleteGraph();
        mMessagesDB.deleteMessageDB();

        return true;
    }

    public boolean closeAllDataBase(){

        mNodesDB.closeNodesDB();
        mInboxDB.getDatabase().close();
        mHandShakeDB.getDatabase().close();
        mGraphRelations.getDatabase().close();
        mMessagesDB.closeMessageDB();
        return true;

    }

    public boolean openAllDataBase(){
        try {

            mNodesDB.openNodesDB();
            if (! mInboxDB.getDatabase().isOpen() ) mInboxDB.getDatabase().open();
            if (! mHandShakeDB.getDatabase().isOpen() ) mHandShakeDB.getDatabase().open();
            if (! mGraphRelations.getDatabase().isOpen() ) mGraphRelations.getDatabase().open();
            mMessagesDB.openMessageDB();

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Transfer all the events that happened before X hours to history log.
     * history log exists for delivering the log data to the server
     * @param hour
     * @return
     */
    public boolean moveOldHandShakEventsToLogInAllNodes(int hour){
        ArrayList<UUID> nodesId = mHandShakeDB.getNodesIdList();
        HandShakeHistory temp;
        for (UUID nodeId : nodesId){
            temp = mHandShakeDB.getHandShakeHistoryWith(nodeId);
            if(temp != null) {
                temp.moveOldHandShakEventsToLog(hour);
                mHandShakeDB.updateHandShakeHistoryWith(nodeId, temp);
            }
        }
        return true;
    }


    /**
     * Delete handshake history log.
     * do it after handshake with the sever
     * @return
     */
    public boolean deleteHandShakeHistoryLogInNodes(){
        ArrayList<UUID> nodesId = mHandShakeDB.getNodesIdList();
        HandShakeHistory temp;
        for (UUID nodeId : nodesId){
            temp = mHandShakeDB.getHandShakeHistoryWith(nodeId);
            temp.deleteHandShakeEventLog();
            // if handshakeCounter is empty delete node from handshakdb;
            if(temp.getmHandShakeCounter() == 0)
                mHandShakeDB.deleteNodeId(nodeId);
            else
                mHandShakeDB.updateHandShakeHistoryWith(nodeId,temp);
        }
        return true;
    }

    /**
     * go over all nodes in graph and delete all the relations with my node that the handshake
     * counter is 0 or not exist.
     * @return
     */
    public boolean deleteOldRelation(){

        HashMap< Integer, ArrayList<UUID>> graphBFS =
                mGraphRelations.bfs(mGraphRelations,mNodesDB.getMyNodeId());
        ArrayList<UUID> uuidFirstDegree = graphBFS.get(1);

        for(UUID uuid : uuidFirstDegree){
            HandShakeHistory handShakeHistory = mHandShakeDB.getHandShakeHistoryWith(uuid);
            // if handshake not exist delete relation with myNode
            if ( handShakeHistory == null ){
                mGraphRelations.deleteRelation(mNodesDB.getMyNodeId(),uuid);
            }else
            // if handshake counter equal to 0 delete relation and delete the node from
            if (handShakeHistory.getmHandShakeCounter() == 0 ){
                mGraphRelations.deleteRelation(mNodesDB.getMyNodeId(),uuid);
            }
        }
        return true;
    }


    /**
     * Delete all the nodes in graph relation and nodeDB that not connected to the node after doing bfs max
     */
    public void deleteAllSeparateNodesFromGraphRelation(){

        HashMap< Integer, ArrayList<UUID>> graphBFS =
                mGraphRelations.bfs(mGraphRelations,mNodesDB.getMyNodeId());

        // build arraylist of graph bfs
        ArrayList<UUID> uuidbfs = new ArrayList<>();
        for (int i = 1; i<graphBFS.size(); i++)
            uuidbfs.addAll(graphBFS.get(i));

        // build arrayList of all node in graph relations
        ArrayList<UUID> uuidInGraphRelations = mGraphRelations.getNodesIdList();

        for(UUID uuid : uuidInGraphRelations){
            // if the arrayBfs not contain this node. delete it.
            if( !uuidbfs.contains(uuid) ) {
                //delete node from graph relation
                mGraphRelations.deleteNode(uuid);
                // TODO in hte future don't delete  node that consider to be user's friends or nodes that sent msg to or received msg from this device
                // delete node from nodeDB
                mNodesDB.deleteNode(uuid);

            }
        }
    }

    /**
     * go over all messages.
     * if the sender id or destination id are not in this node graph relation
     */
    public void deleteAllMessagesNotInGraphRelation(){

        ArrayList<UUID> uuidArrayList = mMessagesDB.getMessagesIdList();
        for (UUID uuid : uuidArrayList){
            RelayMessage msg = mMessagesDB.getMessage(uuid);
            if (!mGraphRelations.hasNode(msg.getDestinationId()) &&
                    !mGraphRelations.hasNode(msg.getSenderId()) ){
                mMessagesDB.deleteMessage(uuid);
            }
        }
    }

    /**
     * Get all handShakeHistory from DB
     * @return
     */
    public ArrayList<HandShakeHistory> getAllHandShakeHistoryList(){
        ArrayList<HandShakeHistory> handShakeHistories = new ArrayList<>();
        ArrayList<UUID> nodesId = mHandShakeDB.getNodesIdList();
        for (UUID nodeId : nodesId){
            handShakeHistories.add(mHandShakeDB.getHandShakeHistoryWith(nodeId));
        }
        return handShakeHistories;
    }


    /**
     * Check if the nodeId is in handshake history
     * @param nodeId
     * @return
     */
    public int getMyHandShakeHistoryRankWith(UUID nodeId){
        HandShakeHistory handShakeHistory  = mHandShakeDB.getHandShakeHistoryWith(nodeId);
        if (handShakeHistory == null)
            return 0; // min
        return handShakeHistory.getmHandShakeRank();
    }


    /**
     * Handler of incoming messages from inboxDB
     */
    class IncomingHandler extends Handler {

        UUID messageUUID;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case DELETE_MESSAGE_CONTENT_FROM_MESSAGE_DB:
                    messageUUID = UUID.fromString( msg.getData().getString("uuid") );
                    RelayMessage message = getMessagesDB().getMessage(messageUUID);
                    message.deleteAttachment();
                    message.deleteContent();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

}
