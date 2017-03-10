package com.relay.relay.DB;

import android.content.Context;

import com.relay.relay.Util.JsonConvertor;
import com.relay.relay.system.Node;

import java.util.ArrayList;
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

    /**
     * Constructor
     * @param context
     * @param graphRelations
     */
    public NodesDB(Context context,GraphRelations graphRelations){
        dbManager = new DBManager(DB,context);
        dbManager.openDB();
        if (!dbManager.isKeyExist(NUM_OF_NODES))
            dbManager.putJsonObject(NUM_OF_NODES, JsonConvertor.convertToJson(0));
        this.graphRelations = graphRelations;
    }

    /**
     * Add Node
     * @param node
     * @return true if success
     */
    public boolean addNode(Node node){
        if (!dbManager.isKeyExist(node.getId())) {
            dbManager.putJsonObject(node.getId(),JsonConvertor.convertToJson(node));
            graphRelations.addNode(node.getId());
            addNumNodes();
            return true;
        }
        else{
            dbManager.putJsonObject(node.getId(), JsonConvertor.convertToJson(node));
            return true;
        }
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
            return JsonConvertor.JsonToNode(dbManager.getJsonObject(uuid));
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
            reduceNumNodes();
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

}
