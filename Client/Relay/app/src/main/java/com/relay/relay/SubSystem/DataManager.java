package com.relay.relay.SubSystem;

import android.content.Context;
import android.util.Log;

import com.relay.relay.DB.GraphRelations;
import com.relay.relay.DB.HandShakeDB;
import com.relay.relay.DB.InboxDB;
import com.relay.relay.DB.MessagesDB;
import com.relay.relay.DB.NodesDB;
import com.relay.relay.Util.DataTransferred;
import com.relay.relay.system.HandShakeHistory;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
import java.util.UUID;

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

    public DataManager(Context context){

        this.context = context;
        this.mGraphRelations = new GraphRelations(context);
        this.mNodesDB = new NodesDB(context,mGraphRelations);
        this.mMessagesDB = new MessagesDB(context);
        this.mHandShakeDB = new HandShakeDB(context);
        this.mGraphRelations.setNodesDB(mNodesDB);
        this.mInboxDB =  new InboxDB(context,mNodesDB.getMyNodeId());
    }

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


    /**
     * transfer all the events that happened before weeks to history log. if there is an empty node
     * without events delete it.
     * @param weeks
     * @return
     */
    public boolean cleanHandShakeHistory(int weeks){
    // TODO add delete all the messages that not relevant
        ArrayList<UUID> nodesId = mHandShakeDB.getNodesIdList();
        HandShakeHistory temp;
        for (UUID nodeId : nodesId){
            temp = mHandShakeDB.getHandShakeHistoryWith(nodeId);
            temp.cleanHandShakeEvents(weeks);
            mHandShakeDB.updateHandShakeHistoryWith(nodeId,temp);
        }
        return true;
    }

    /**
     * Clear handshake history log
     * @return
     */
    public boolean clearHandShakeHistoryLog(){
        ArrayList<UUID> nodesId = mHandShakeDB.getNodesIdList();
        HandShakeHistory temp;
        for (UUID nodeId : nodesId){
            temp = mHandShakeDB.getHandShakeHistoryWith(nodeId);
            temp.clearHandShakeEventLog();
            // if
            if(temp.getmHandShakeCounter() == 0)
                mHandShakeDB.deleteNodeId(nodeId);
            else
                mHandShakeDB.updateHandShakeHistoryWith(nodeId,temp);
        }
        return true;
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
     * go over all messages. if the sender id or destination id not in node graph. delete msg
     */
    public void deleteAllMessagesOfUnkownNodes(){

        ArrayList<UUID> uuidArrayList = mMessagesDB.getMessagesIdList();
        for (UUID uuid : uuidArrayList){
            RelayMessage msg = mMessagesDB.getMessage(uuid);
            if (!mNodesDB.isNodeExist(msg.getDestinationId()) &&
                    !mNodesDB.isNodeExist(msg.getSenderId()) ){
                mMessagesDB.deleteMessage(uuid);
            }
        }
    }

}
