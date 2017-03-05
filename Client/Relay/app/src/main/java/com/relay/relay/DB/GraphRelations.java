package com.relay.relay.DB;
import android.content.Context;

import com.relay.relay.Util.JsonConvertor;

import java.util.*;
import java.util.Queue;

public class GraphRelations {

    final String TAG = "RELAY_DEBUG: "+ GraphRelations.class.getSimpleName();
    private DBManager dbManager;
    final String DB = "graph_relations";
    final UUID NUM_OF_NODES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b515");
    final UUID NUM_OF_EDGES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b513");
    private static final ArrayList<UUID> EMPTY_SET = new ArrayList<>();

    /**
     * Construct empty GraphRelations
     */
    public GraphRelations(Context context) {
        dbManager = new DBManager(DB,context);
        dbManager.openDB();
    }

    public boolean deleteGraph(){
        return dbManager.deleteDB();
    }


    /**
     * Add a new node with no neighbors
     */
    public boolean addNode(UUID uuid) {
        final UUID node = uuid;
        if (!hasNode(node)) {
            dbManager.putJsonObject(uuid,JsonConvertor.ConvertToJson(new ArrayList<String>()));
            addNumNodes();
            return true;
        }
        return false;
    }

    /**
     * Returns true iff v is in this GraphRelations, false otherwise
     */
    public boolean hasNode(UUID uuid) {
        return dbManager.isKeyExist(uuid) ;
    }

    /**
     * Is from-to, an edge in this GraphRelations. The graph is
     * undirected so the order of from and to does not
     * matter.
     */
    public boolean hasEdge(UUID from, UUID to) {

        ArrayList<UUID> temp = null;
        if (!hasNode(from) || !hasNode(to))
            return false;
        temp = JsonConvertor.JsonToUUIDArrayList(dbManager.getJsonObject(from));
        return temp.contains(to);
    }

    /**
     * Add to to from's set of neighbors, and add from to to's
     * set of neighbors. Does not add an edge if another edge
     * already exists or both node are not exist;
     */

    public boolean addEdge(UUID from, UUID to) {

        ArrayList<UUID> temp = null;

        if (!hasNode(from) || !hasNode(to))
            return false;
        if (hasEdge(from, to))
            return false;
        if (from == to)
            return false;
//        addNode(from);
//        addNode(to);
        addNumEdges();

        temp = JsonConvertor.JsonToUUIDArrayList(dbManager.getJsonObject(from));
        temp.add(to);
        dbManager.putJsonObject(from,JsonConvertor.ConvertToJson(temp));

        temp = JsonConvertor.JsonToUUIDArrayList(dbManager.getJsonObject(to));
        temp.add(from);
        dbManager.putJsonObject(to,JsonConvertor.ConvertToJson(temp));

        return true;
    }


    /**
     * Return an iterator over the neighbors of UUID
     */
    public ArrayList<UUID> adjacentTo(UUID uuid) {
 //       ArrayList<UUID> arrayList = new ArrayList<>();
        if (!hasNode(uuid))
            return EMPTY_SET;
//        // the system has problem with list with UUID,therefor i put string and the conver it to uuid
//        for(String s : (ArrayList<String>)dbManager.getJsonObject(uuid)){
//            arrayList.add(UUID.fromString(s));
//        }
        return JsonConvertor.JsonToUUIDArrayList(dbManager.getJsonObject(uuid));
    }


    public void addNumNodes(){

        if (!dbManager.isKeyExist(NUM_OF_NODES)){
            dbManager.putJsonObject(NUM_OF_NODES,JsonConvertor.ConvertToJson(1));
        }
        else{
            int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
            num++;
            dbManager.putJsonObject(NUM_OF_NODES,JsonConvertor.ConvertToJson(num));
        }
    }

    public void addNumEdges(){
        if (!dbManager.isKeyExist(NUM_OF_EDGES)){
            dbManager.putJsonObject(NUM_OF_EDGES,JsonConvertor.ConvertToJson(1));
        }
        else{
            int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_EDGES));
            num++;
            dbManager.putJsonObject(NUM_OF_EDGES,JsonConvertor.ConvertToJson(num));
        }
    }

    public void reduceNumEdges(){
        if (!dbManager.isKeyExist(NUM_OF_EDGES)){
           return;
        }
        else{
            int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_EDGES));
            num--;
            if (num>=0)
                dbManager.putJsonObject(NUM_OF_EDGES,JsonConvertor.ConvertToJson(num));
        }
    }

    public void reduceNumNodes(){
        if (!dbManager.isKeyExist(NUM_OF_NODES)){
            return;
        }
        else{
            int num = JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
            num--;
            if (num>=0)
                dbManager.putJsonObject(NUM_OF_NODES,JsonConvertor.ConvertToJson(num));
        }
    }

    public int getMyNumNodes() {

        if (!dbManager.isKeyExist(NUM_OF_NODES)){
            return  0;
        }
        else{
            return JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_NODES));
        }
    }

    public int getMyNumEdges() {
        if (!dbManager.isKeyExist(NUM_OF_EDGES)) {
            return 0;
        } else {
            return JsonConvertor.JsonToInt(dbManager.getJsonObject(NUM_OF_EDGES));
        }
    }

    public boolean deleteNode(UUID uuid){
        if (!dbManager.isKeyExist(uuid)) {
            return false;
        } else {
            ArrayList<UUID> arrayList = adjacentTo(uuid);
            while(arrayList.size()>0){
                deleteRelation(arrayList.get(0),uuid);
            }
            dbManager.deleteJsonObject(uuid);
            reduceNumNodes();
            return true;
        }
    }

    public boolean deleteRelation(UUID from,UUID to){

        ArrayList<UUID> temp = null;

        if (!hasEdge(from, to))
            return false;
        if (from == to)
            return false;
        reduceNumEdges();


        temp = JsonConvertor.JsonToUUIDArrayList(dbManager.getJsonObject(from));
        temp.remove(to);
        dbManager.putJsonObject(from,JsonConvertor.ConvertToJson(temp));

        temp = JsonConvertor.JsonToUUIDArrayList(dbManager.getJsonObject(to));
        temp.remove(from);
        dbManager.putJsonObject(from,JsonConvertor.ConvertToJson(to));
        return true;
    }

    /**
     * breadth-first search from a single source
     * returns hashMap of graphRelations Ordered By Degree
     */
    public HashMap< Integer, ArrayList<UUID>> bfs(GraphRelations graphRelations, UUID s) {



        final int INFINITY = Integer.MAX_VALUE;
        boolean[] marked;  // marked[v] = is there an s-v path
        int[] edgeTo;      // edgeTo[v] = previous edge on shortest s-v path
        int[] distTo;      // distTo[v] = number of edges shortest s-v path
        UUID[] nodesArray;  // array of nodes


        marked = new boolean[graphRelations.getMyNumNodes()];
        distTo = new int[graphRelations.getMyNumNodes()];
        edgeTo = new int[graphRelations.getMyNumNodes()];

        ArrayList<UUID> nodesArrayList = dbManager.getKyes();
        nodesArrayList.remove(NUM_OF_EDGES);
        nodesArrayList.remove(NUM_OF_NODES);

        HashMap< Integer, ArrayList<UUID>> graphOrderedByDegree = new HashMap<>();

        if (graphRelations.hasNode(s))
        {
            Queue<UUID> q = new java.util.ArrayDeque<UUID>();
            for (int i = 0; i < graphRelations.getMyNumNodes(); i++)
                distTo[i] = INFINITY;
            distTo[nodesArrayList.indexOf(s)] = 0;
            marked[nodesArrayList.indexOf(s)] = true;
            q.add(s);

            while (!q.isEmpty()) {
                UUID v = q.remove();

                // if degree exist? add it to the graphOrderedByDegree
                if (graphOrderedByDegree.containsKey(distTo[nodesArrayList.indexOf(v)]))
                {
                    ArrayList<UUID> tempList = graphOrderedByDegree.get(distTo[nodesArrayList.indexOf(v)]);
                    tempList.add(v);
                    graphOrderedByDegree.put(distTo[nodesArrayList.indexOf(v)],tempList);
                }
                else{
                    ArrayList<UUID> tempList = new ArrayList<>();
                    tempList.add(v);
                    graphOrderedByDegree.put(distTo[nodesArrayList.indexOf(v)],tempList);
                }

                for (UUID w : graphRelations.adjacentTo(v)) {
                    if (!marked[nodesArrayList.indexOf(w)]) {
                        edgeTo[nodesArrayList.indexOf(w)] = nodesArrayList.indexOf(v);
                        distTo[nodesArrayList.indexOf(w)]= distTo[nodesArrayList.indexOf(v)] + 1;
                        marked[nodesArrayList.indexOf(w)] = true;
                        q.add(w);
                    }
                }
            }
        }
        return graphOrderedByDegree;
    }
}