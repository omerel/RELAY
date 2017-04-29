package com.relay.relay.DB;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.JsonConvertor;
import com.relay.relay.system.Node;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by omer on 01/03/2017.
 * NodeDB saves all the Nodes information in the device;
 */

public class NodesDB {

    final String TAG = "RELAY_DEBUG: "+ NodesDB.class.getSimpleName();
    final UUID NUM_OF_NODES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b515");
    final UUID MY_NODE_ID = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b214");
    private DBManager dbManager;
    private GraphRelations graphRelations;
    final String DB = "nodes_db";
    private InboxDB mInboxDB;
    private AttachmentsDB mAttachmentsDB;

    // TODO add NODE for server

    /**
     * Constructor
     * @param context
     * @param graphRelations
     */
    public NodesDB(Context context,GraphRelations graphRelations,InboxDB inboxDB){
        dbManager = new DBManager(DB,context);
        dbManager.openDB();
        if (!dbManager.isKeyExist(NUM_OF_NODES))
            dbManager.putJsonObject(NUM_OF_NODES, JsonConvertor.convertToJson(0));
        this.graphRelations = graphRelations;
        this.mInboxDB = inboxDB;
        mAttachmentsDB = new AttachmentsDB(context);
    }

    /**
     * Add Node
     * @param node
     * @return true if success
     */
    public boolean addNode(Node node){

        Log.e(TAG,"add node");
        if (!dbManager.isKeyExist(node.getId())) {

            byte[]  bytes = null;
            if (node.getProfilePicture() != null){
                // separate image from node. add node to node db
                bytes = node.getProfilePicture();
                node.setProfilePicture(null);
            }

            // add node to nodesDB
            dbManager.putJsonObject(node.getId(),JsonConvertor.convertToJson(node));

            Log.e(TAG,"Add node to nodeDB");
            // update attachmentDB
            if (bytes != null) {
                Log.e(TAG,"Add Attachment to DB ");
                mAttachmentsDB.addAttachment(node.getId(), new ByteArrayInputStream(bytes));
            }
            // update graphRelations
            graphRelations.addNode(node.getId());
            addNumNodes();

            // update inboxDB
            String keySearch = node.getFullName()+mInboxDB.SEARCH_KEY_DELIMITER+
                    node.getUserName()+mInboxDB.SEARCH_KEY_DELIMITER+
                    node.getEmail();
            mInboxDB.addContactItem(node.getId(),keySearch);
            return true;
        }
        else{
            Log.e(TAG,"update node without rankServer");
            Node oldNode = getNode(node.getId());
            int oldRank = oldNode.getRank();
            Calendar oldTimeStampRank = oldNode.getTimeStampRankFromServer();
            node.setRank(oldRank,oldTimeStampRank);

            // separate image from node. add node to node db
            byte[]  bytes = node.getProfilePicture();
            node.setProfilePicture(null);

            Log.e(TAG,"Update node to nodeDB");
            dbManager.putJsonObject(node.getId(), JsonConvertor.convertToJson(node));

            // update attachmentDB
            if (bytes != null) {
                Log.e(TAG,"Add Attachment to DB ");
                mAttachmentsDB.addAttachment(node.getId(), new ByteArrayInputStream(bytes));
            }

            // update inboxDB
            String keySearch = node.getFullName()+mInboxDB.SEARCH_KEY_DELIMITER+
                    node.getUserName()+mInboxDB.SEARCH_KEY_DELIMITER+
                    node.getEmail();
            mInboxDB.addContactItem(node.getId(),keySearch);

            //mInboxDB.updateContactItem(node.getId(),"",false,false,false,true);
            return true;
        }
    }

    public boolean updateNodeRank(UUID nodeID,int rank,Calendar timeStampRankFromServer){
        Log.e(TAG,"update node rank");
        if (dbManager.isKeyExist(nodeID)) {
            Node node = getNode(nodeID);
            node.setRank(rank,timeStampRankFromServer);
            // remove attachment
            node.setProfilePicture(null);
            dbManager.putJsonObject(node.getId(), JsonConvertor.convertToJson(node));
            return true;
        }
        return false;
    }

    public boolean isNodeExist(UUID nodeId){
         return dbManager.isKeyExist(nodeId);
    }

    /**
     * Get Node
     * @param uuid
     * @return Node if found, Null if not found
     */
    public Node getNode(UUID uuid){
        if (dbManager.isKeyExist(uuid)){
            Node node = JsonConvertor.JsonToNode(dbManager.getJsonObject(uuid));
            if (mAttachmentsDB.getAttachment(uuid) != null)
                node.setProfilePicture(ImageConverter.convertInputStreamToByteArray(mAttachmentsDB.getAttachment(uuid)));
            return  node;
        }
        else
            return null;
    }

    /**
     * delete Node from database
     * @param uuid
     * @return
     */
    public boolean deleteNode(UUID uuid){
        if (dbManager.isKeyExist(uuid)){
            dbManager.deleteJsonObject(uuid);
            graphRelations.deleteNode(uuid);
            // TODO delete attachment of node?
            reduceNumNodes();

            mInboxDB.deleteContactFromDB(uuid);

            mAttachmentsDB.deleteAttachment(uuid);
            return true;
        }
        else
            return false;
    }

    /**
     * Get nodes list
     * @return arraylist
     */
    public ArrayList<UUID> getNodesIdList(){

        ArrayList<UUID> temp = dbManager.getKyes();
        temp.remove(NUM_OF_NODES);
        temp.remove(MY_NODE_ID);
        return temp;

    }

    /**
     * Add to nodes counter
     */
    private void addNumNodes(){
        int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
        num++;
        dbManager.putJsonObject(NUM_OF_NODES,JsonConvertor.convertToJson(num));
    }

    /**
     * Reduce from node counter
     */
    private void reduceNumNodes(){
        int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
        num--;
        if (num >= 0)
            dbManager.putJsonObject(NUM_OF_NODES,JsonConvertor.convertToJson(num));
    }

    /**
     * Get nodes counter
     * @return
     */
    public int getNumNodes() {
        return JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
    }

    /**
     * Delete nodes database
     * @return
     */
    public boolean deleteNodedb(){
        mAttachmentsDB.deleteDB();
        return dbManager.deleteDB();
    }


    /**
     * Add MY Node
     * @param nodeId
     * @return true if success
     */
    public boolean setMyNodeId(UUID nodeId){
        dbManager.putJsonObject(MY_NODE_ID,JsonConvertor.convertToJson(nodeId));
        return true;
    }

    /**
     * Get MY Node
     */
    public UUID getMyNodeId(){
        return JsonConvertor.JsonToUUID(dbManager.getJsonObject(MY_NODE_ID));
    }

    public Database getDatabase(){return dbManager.getDatabase();}

    public boolean closeNodesDB(){
        mAttachmentsDB.getDatabase().close();
        dbManager.getDatabase().close();
        return true;
    }
    public boolean openNodesDB(){
        try {
            if ( ! mAttachmentsDB.getDatabase().isOpen()) mAttachmentsDB.getDatabase().open();
            if ( ! dbManager.getDatabase().isOpen() ) dbManager.getDatabase().open();
            return true;
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return false;
    }
}
