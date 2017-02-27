package com.relay.relay.Util;

import com.relay.relay.system.Node;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by omer on 19/02/2017.
 */

public class Data {

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

    public class KnownRelations{
        private UUID nodeId;
        private Date timeStampNodeDetails;
        private Date timeStampNodeRelations;
        private int nodeDegree;

        public KnownRelations(UUID nodeId, Date timeStampNodeDetails,
                              Date timeStampNodeRelations, int nodeDegree){
            this.nodeId = nodeId;
            this.timeStampNodeDetails = timeStampNodeDetails;
            this.timeStampNodeRelations = timeStampNodeRelations;
            this.nodeDegree = nodeDegree;

        }

        public UUID getNodeId() {
            return nodeId;
        }

        public Date getTimeStampNodeDetails() {
            return timeStampNodeDetails;
        }

        public Date getTimeStampNodeRelations() {
            return timeStampNodeRelations;
        }

        public int getNodeDegree() {
            return nodeDegree;
        }
    }

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


    public class NodeRelations{
        private UUID nodeId;
        private Date timeStampNodeRelations;
        private ArrayList<UUID> relations;

        public NodeRelations(UUID nodeId, Date timeStampNodeRelations,
                             ArrayList<UUID> relations) {
            this.nodeId = nodeId;
            this.timeStampNodeRelations = timeStampNodeRelations;
            this.relations = relations;
        }

        public UUID getNodeId() {
            return nodeId;
        }

        public Date getTimeStampNodeRelations() {
            return timeStampNodeRelations;
        }

        public ArrayList<UUID> getRelations() {
            return relations;
        }
    }

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
