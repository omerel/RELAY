package com.relay.relay.Util;

import com.relay.relay.DB.GraphRelations;
import com.relay.relay.DB.MessagesDB;
import com.relay.relay.DB.NodesDB;
import com.relay.relay.system.Node;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 19/02/2017.
 * DataTransferred is the data that are transferred in the handshake
 */

public class DataTransferred {

    final int MAXDEGREE = 6;
    private Node mMyNode;
    private GraphRelations mGraphRelations;
    private NodesDB mNodesDB;
    private MessagesDB mMessagesDB;

    /**
     * Counstructor to create manager
     * @param graphRelations
     * @param nodesDB
     * @param messagesDB
     */
    public DataTransferred(GraphRelations graphRelations,NodesDB nodesDB,
                           MessagesDB messagesDB){
        this.mGraphRelations = graphRelations;
        this.mNodesDB = nodesDB;
        this.mMessagesDB = messagesDB;
        this.mMyNode = mNodesDB.getNode(mNodesDB.getMyNodeId());
    }

    /**
     * Create Meta data to transfer
     * @return
     */
    public Metadata createMetaData(){
        return new Metadata(mMyNode,
                createKnownRelationsList(mMyNode,mGraphRelations,mNodesDB),
                createKnownMessagesList(mMessagesDB));
    }

    public NodeRelations createNodeRelations(UUID nodeId,Calendar timeStampNodeRelations,
                                             ArrayList<UUID> relations){
        return new NodeRelations(nodeId,timeStampNodeRelations,relations);
    }

    /**
     * Create Known Relations List
     * @param myNode
     * @param graphRelations
     * @param nodesDB
     * @return
     */
    private Map<UUID,KnownRelations> createKnownRelationsList( Node myNode,
                                      GraphRelations graphRelations, NodesDB nodesDB){
        int degree = MAXDEGREE;
        Map<UUID,KnownRelations> knownRelationsArrayList = new HashMap<>();
        ArrayList<UUID> uuidArrayList;
        HashMap< Integer, ArrayList<UUID>> bfs = graphRelations.bfs(graphRelations, myNode.getId());

        // set min(maxdegree,bfs.size)
        if ( bfs.size() < degree)
            degree = bfs.size();

        for (int i = 0; i < degree; i++){
            uuidArrayList = bfs.get(i);
            for(int j =0; j< uuidArrayList.size(); j++){
                Node tempNode = nodesDB.getNode(uuidArrayList.get(j));
                knownRelationsArrayList.put(tempNode.getId(),new KnownRelations(tempNode.getId(),
                        tempNode.getTimeStampNodeDetails(),tempNode.getTimeStampNodeRelations(),
                        i,tempNode.getRank(),tempNode.getTimeStampRankFromServer()));
            }
        }
        return knownRelationsArrayList;
    }

    /**
     * Create known Messages List
     * @param messagesDB
     * @return
     */
    private Map<UUID,KnownMessage> createKnownMessagesList(MessagesDB messagesDB){
        Map<UUID,KnownMessage> knownMessageArrayList = new HashMap<>();
        ArrayList<UUID> uuidArrayList = messagesDB.getMessagesIdList();

        for (int i = 0 ; i < uuidArrayList.size(); i++ ){
            knownMessageArrayList.put(uuidArrayList.get(i),new KnownMessage(uuidArrayList.get(i),
                    messagesDB.getMessage(uuidArrayList.get(i)).getStatus()));
        }
        return  knownMessageArrayList;
    }

    /**
     * Meta data class
     */
    public class Metadata{
        private Node myNode;
        private Map<UUID,KnownRelations> knownRelationsList;
        private Map<UUID,KnownMessage> knownMessagesList;

        public Metadata(Node myNode, Map<UUID,KnownRelations> knownRelationsList,
                        Map<UUID,KnownMessage> knownMessagesList) {
            this.myNode = myNode;
            this.knownRelationsList = knownRelationsList;
            this.knownMessagesList = knownMessagesList;
        }

        public Node getMyNode() {
            return myNode;
        }

        public Map<UUID,KnownRelations> getKnownRelationsList() {
            return knownRelationsList;
        }

        public Map<UUID,KnownMessage> getKnownMessagesList() {
            return knownMessagesList;
        }
    }

    /**
     * KnownRelations class
     */
    public class KnownRelations{
        private UUID nodeId;
        private Calendar timeStampNodeDetails;
        private Calendar timeStampNodeRelations;
        private Calendar timeStampRankFromServer;
        private int nodeDegree;
        private int rank;

        public KnownRelations(UUID nodeId, Calendar timeStampNodeDetails,
                              Calendar timeStampNodeRelations, int nodeDegree,int rank,Calendar timeStampRankFromServer){
            this.nodeId = nodeId;
            this.timeStampNodeDetails = timeStampNodeDetails;
            this.timeStampNodeRelations = timeStampNodeRelations;
            this.timeStampRankFromServer = timeStampRankFromServer;
            this.nodeDegree = nodeDegree;
            this.rank = rank;
        }

        public UUID getNodeId() {
            return nodeId;
        }

        public int getNodeRank() {
            return rank;
        }

        public Calendar getTimeStampNodeDetails() {
            return timeStampNodeDetails;
        }

        public Calendar getTimeStampNodeRelations() {
            return timeStampNodeRelations;
        }

        public Calendar getTimeStampRankFromServer(){return timeStampRankFromServer;}

        public int getNodeDegree() {
            return nodeDegree;
        }
    }

    /**
     * KnownMessage class
     */
    public class KnownMessage{
        private UUID messageId;
        private int status;

        public KnownMessage(UUID messageId,int status){
            this.messageId = messageId;
            this.status = status;
        }

        public UUID getMessageId() {
            return messageId;
        }

        public int getStatus() {
            return status;
        }
    }

    /**
     * NodeRelations class
     */
    public class NodeRelations{
        private UUID nodeId;
        private Calendar timeStampNodeRelations;
        private ArrayList<UUID> relations;

        public NodeRelations(UUID nodeId, Calendar timeStampNodeRelations,
                             ArrayList<UUID> relations) {
            this.nodeId = nodeId;
            this.timeStampNodeRelations = timeStampNodeRelations;
            this.relations = relations;
        }

        public UUID getNodeId() {
            return nodeId;
        }

        public Calendar getTimeStampNodeRelations() {
            return timeStampNodeRelations;
        }

        public ArrayList<UUID> getRelations() {
            return relations;
        }
    }


    public UpdateNodeAndRelations createUpdateNodeAndRelations
                                            (ArrayList<Node> nodeList,
                                             ArrayList<NodeRelations> relationsList  ){

        return new UpdateNodeAndRelations(nodeList,relationsList);
    }

    /**
     * UpdateNodeAndRelations class
     */
    public class UpdateNodeAndRelations{
        private ArrayList<Node> nodeList;
        private ArrayList<NodeRelations> relationsList;

        public UpdateNodeAndRelations(ArrayList<Node> nodeList,
                                      ArrayList<NodeRelations> relationsList) {
            this.nodeList = nodeList;
            this.relationsList = relationsList;
        }

        public ArrayList<Node> getNodeList() {
            return nodeList;
        }

        public ArrayList<NodeRelations> getRelationsList() {
            return relationsList;
        }
    }

}
