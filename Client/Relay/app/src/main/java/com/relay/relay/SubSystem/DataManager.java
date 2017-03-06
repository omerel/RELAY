package com.relay.relay.SubSystem;

import com.relay.relay.DB.GraphRelations;
import com.relay.relay.DB.MessagesDB;
import com.relay.relay.DB.NodesDB;
import com.relay.relay.system.HandShakeHistory;

/**
 * Created by omer on 05/03/2017.
 * Data manager responsible for maintenance of the saved data.
 */

public class DataManager {


    private GraphRelations mGraphRelations;
    private MessagesDB mMessagesDB;
    private NodesDB mNodesDB;
    private HandShakeHistory mHandShakeHistory;

    public DataManager(GraphRelations graphRelations,
                       MessagesDB messagesDB, NodesDB nodesDB, HandShakeHistory handShakeHistory){

        this.mGraphRelations = graphRelations;
        this.mHandShakeHistory = handShakeHistory;
        this.mMessagesDB = messagesDB;
        this.mNodesDB = nodesDB;

    }
}
