package com.relay.relay.Util;

import com.relay.relay.DB.GraphRelations;
import com.relay.relay.DB.MessagesDB;
import com.relay.relay.DB.NodesDB;
import com.relay.relay.system.Node;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
                createknownMessagesList(mMessagesDB));
    }

    /**
     * Create Known Relations List
     * @param myNode
     * @param graphRelations
     * @param nodesDB
     * @return
     */
    private ArrayList<KnownRelations> createKnownRelationsList( Node myNode,
                                      GraphRelations graphRelations, NodesDB nodesDB){
        int degree = MAXDEGREE;
        ArrayList<KnownRelations> knownRelationsArrayList = new ArrayList<>();
        ArrayList<UUID> uuidArrayList;
        HashMap< Integer, ArrayList<UUID>> bfs = graphRelations.bfs(graphRelations, myNode.getId());

        // set min(maxdegree,bfs.size
        if ( bfs.size() < degree)
            degree = bfs.size();

        for (int i = 1; i < degree; i++){
            uuidArrayList = bfs.get(i);
            for(int j =0; j< uuidArrayList.size(); j++){
                Node tempNode = nodesDB.getNode(uuidArrayList.get(j));
                knownRelationsArrayList.add(new KnownRelations(tempNode.getId(),
                        tempNode.getTimeStampNodeDetails(),tempNode.getTimeStampNodeRelations(),
                        i,tempNode.getRank()));
            }
        }
        return knownRelationsArrayList;
    }

    /**
     * Create known Messages List
     * @param messagesDB
     * @return
     */
    private ArrayList<KnownMessage> createknownMessagesList(MessagesDB messagesDB){
        ArrayList<KnownMessage> knownMessageArrayList = new ArrayList<>();
        ArrayList<UUID> uuidArrayList = messagesDB.getMessagesIdList();

        for (int i = 0 ; i < uuidArrayList.size(); i++ ){
            knownMessageArrayList.add(new KnownMessage(uuidArrayList.get(i),
                    messagesDB.getMessage(uuidArrayList.get(i)).getStatus()));
        }
        return  knownMessageArrayList;
    }

    /**
     * Meta data class
     */
    public class Metadata{
        private Node myNode;
        private ArrayList<KnownRelations> knownRelationsList;
        private ArrayList<KnownMessage> knownMessagesList;

        public Metadata(Node myNode, ArrayList<KnownRelations> knownRelationsList,
                        ArrayList<KnownMessage> knownMessagesList) {
            this.myNode = myNode;
            this.knownRelationsList = knownRelationsList;
            this.knownMessagesList = knownMessagesList;
        }

        public Node getMyNode() {
            return myNode;
        }

        public ArrayList<KnownRelations> getKnownRelationsList() {
            return knownRelationsList;
        }

        public ArrayList<KnownMessage> getKnownMessagesList() {
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
        private int nodeDegree;
        private int rank;

        public KnownRelations(UUID nodeId, Calendar timeStampNodeDetails,
                              Calendar timeStampNodeRelations, int nodeDegree,int rank){
            this.nodeId = nodeId;
            this.timeStampNodeDetails = timeStampNodeDetails;
            this.timeStampNodeRelations = timeStampNodeRelations;
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

    private ArrayList<Node> createNodeToUpdateList(ArrayList<UUID> uuidArrayList,NodesDB nodesDB){
        ArrayList<Node> nodeArrayList = new ArrayList<>();
        for (int i = 0; i<uuidArrayList.size(); i++){
            nodeArrayList.add(nodesDB.getNode(uuidArrayList.get(i)));
        }
        return nodeArrayList;
    }

    private ArrayList<NodeRelations> CreateRelationsList(ArrayList<UUID> uuidArrayList,
                                         NodesDB nodesDB,GraphRelations graphRelations){

        ArrayList<NodeRelations> nodeRelationsArrayList = new ArrayList<>();
        for (int i = 0; i<uuidArrayList.size(); i++){
            Node temp = nodesDB.getNode(uuidArrayList.get(i));
            nodeRelationsArrayList.add(new NodeRelations(temp.getId(),
                    temp.getTimeStampNodeRelations(),graphRelations.adjacentTo(temp.getId())));
        }
        return nodeRelationsArrayList;
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

    public UpdateNodeAndRelations createUpdateNodeAndRelations(){

        ArrayList<UUID> uuidArrayList = mNodesDB.getNodesIdList();
        return new UpdateNodeAndRelations(createNodeToUpdateList(uuidArrayList,mNodesDB),
                CreateRelationsList(uuidArrayList,mNodesDB,mGraphRelations));
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
