package com.relay.relay.DB;

import android.content.Context;

import com.relay.relay.system.Node;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by omer on 01/03/2017.
 */

public class NodesDB {

    final String TAG = "RELAY_DEBUG: "+ NodesDB.class.getSimpleName();
    final UUID NUM_OF_NODES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b515");
    private DBManager dbManager;
    private GraphRelations graphRelations;
    final String DB = "nodes_db";

    public NodesDB(Context context,GraphRelations graphRelations){
        dbManager = new DBManager(DB,context);
        dbManager.openDB();
        dbManager.putObject(NUM_OF_NODES,0);
        this.graphRelations = graphRelations;
    }

    public boolean addNode(Node node){
        if (!dbManager.isKeyExist(node.getId())) {
            dbManager.putObject(node.getId(), node);
            graphRelations.addNode(node.getId());
            addNumNodes();
            return true;
        }
        else{
            dbManager.putObject(node.getId(), node);
            return true;
        }
    }

    public Node getNode(UUID uuid){
        if (dbManager.isKeyExist(uuid))
            return (Node) dbManager.getObject(uuid);
        else
            return null;
    }

    public boolean deleteNode(UUID uuid){
        if (dbManager.isKeyExist(uuid)){
            dbManager.deleteObject(uuid);
            graphRelations.deleteNode(uuid);
            reduceNumNodes();
            return true;
        }
        else
            return false;
    }

    public ArrayList<UUID> getNodesIdList(){
        ArrayList<UUID> temp = dbManager.getKyes();
        temp.remove(NUM_OF_NODES);
        return temp;

    }

    private void addNumNodes(){
        int num = (int) dbManager.getObject(NUM_OF_NODES);
        num++;
        dbManager.putObject(NUM_OF_NODES,num);
    }
    private void reduceNumNodes(){
        int num = (int) dbManager.getObject(NUM_OF_NODES);
        num--;
        if (num >= 0)
            dbManager.putObject(NUM_OF_NODES,num);
    }

    public int getNumNodes() {
        return(int) dbManager.getObject(NUM_OF_NODES);
    }

    public boolean deleteNodedb(){
        return dbManager.deleteDB();
    }


}
