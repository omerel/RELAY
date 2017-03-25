package com.relay.relay.SubSystem;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import com.relay.relay.DB.GraphRelations;
import com.relay.relay.DB.HandShakeDB;
import com.relay.relay.DB.InboxDB;
import com.relay.relay.DB.MessagesDB;
import com.relay.relay.DB.NodesDB;
import com.relay.relay.system.HandShakeHistory;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
import java.util.UUID;

import static com.relay.relay.DB.InboxDB.DELETE_MESSAGE_CONTENT_FROM_MESSAGE_DB;

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
    // Handler for incoming messages from inboxDB
    private final Messenger mMessengerFromDB = new Messenger(new IncomingHandler());

    public DataManager(Context context){

        this.context = context;
        this.mInboxDB =  new InboxDB(context,mNodesDB.getMyNodeId(), mMessengerFromDB);
        this.mGraphRelations = new GraphRelations(context);
        this.mNodesDB = new NodesDB(context,mGraphRelations,mInboxDB);
        this.mMessagesDB = new MessagesDB(context,mInboxDB);
        this.mHandShakeDB = new HandShakeDB(context);
        this.mGraphRelations.setNodesDB(mNodesDB);

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
                    message.deleteAttachments();
                    message.deleteContent();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

}
