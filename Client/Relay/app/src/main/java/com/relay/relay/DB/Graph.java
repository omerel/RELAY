package com.relay.relay.DB;
import android.content.Context;

import java.util.*;
import java.util.Queue;

public class Graph {

    final String TAG = "RELAY_DEBUG: "+ Graph.class.getSimpleName();
    private DBManager dbManager;
    final String DB = "my_graph";
    final UUID NUM_OF_NODES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b515");
    final UUID NUM_OF_EDGES = UUID.fromString("3add4bd4-836f-4ee9-a728-a815c534b513");
    private static final ArrayList<UUID> EMPTY_SET = new ArrayList<>();

    /**
     * Construct empty Graph
     */
    public Graph(Context context) {
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
            dbManager.putObject(uuid, new ArrayList<UUID>());
            addNumNodes();
            return true;
        }
        return false;
    }

    /**
     * Returns true iff v is in this Graph, false otherwise
     */
    public boolean hasNode(UUID uuid) {
        return dbManager.isKeyExist(uuid) ;
    }

    /**
     * Is from-to, an edge in this Graph. The graph is
     * undirected so the order of from and to does not
     * matter.
     */
    public boolean hasEdge(UUID from, UUID to) {

        ArrayList<UUID> temp = null;
        if (!hasNode(from) || !hasNode(to))
            return false;
        temp = (ArrayList<UUID>)dbManager.getObject(from);
        return temp.contains(to);
    }

    /**
     * Add to to from's set of neighbors, and add from to to's
     * set of neighbors. Does not add an edge if another edge
     * already exists
     */

    public void addEdge(UUID from, UUID to) {

        ArrayList<UUID> temp = null;

        if (hasEdge(from, to))
            return;
        if (from == to)
            return;
        addNode(from);
        addNode(to);
        addNumEdges();

        temp = (ArrayList<UUID>) dbManager.getObject(from);
        temp.add(to);
        dbManager.putObject(from,temp);

        temp = (ArrayList<UUID>) dbManager.getObject(to);
        temp.add(from);
        dbManager.putObject(to,temp);
    }


    /**
     * Return an iterator over the neighbors of UUID
     */
    public Iterable<UUID> adjacentTo(UUID uuid) {
        //if (!myGraph.containsKey(uuid))
        if (!hasNode(uuid))
            return EMPTY_SET;
        return (ArrayList<UUID>) dbManager.getObject(uuid);
    }


    public void addNumNodes(){

        if (!dbManager.isKeyExist(NUM_OF_NODES)){
            dbManager.putObject(NUM_OF_NODES,1);
        }
        else{
            int num = (int) dbManager.getObject(NUM_OF_NODES);
            num++;
            dbManager.putObject(NUM_OF_NODES,num);
        }
    }

    public void addNumEdges(){
        if (!dbManager.isKeyExist(NUM_OF_EDGES)){
            dbManager.putObject(NUM_OF_EDGES,1);
        }
        else{
            int num = (int) dbManager.getObject(NUM_OF_EDGES);
            num++;
            dbManager.putObject(NUM_OF_EDGES,num);
        }
    }

    public int getMyNumNodes() {

        if (!dbManager.isKeyExist(NUM_OF_NODES)){
            return  0;
        }
        else{
            return(int) dbManager.getObject(NUM_OF_NODES);
        }
    }

    public int getMyNumEdges() {
        if (!dbManager.isKeyExist(NUM_OF_EDGES)) {
            return 0;
        } else {
            return (int) dbManager.getObject(NUM_OF_EDGES);
        }
    }


    /**
     * breadth-first search from a single source
     * returns hashMap of graph Ordered By Degree
     */
    public HashMap< Integer, ArrayList<UUID>> bfs(Graph graph, UUID s) {

        final int INFINITY = Integer.MAX_VALUE;
        boolean[] marked;  // marked[v] = is there an s-v path
        int[] edgeTo;      // edgeTo[v] = previous edge on shortest s-v path
        int[] distTo;      // distTo[v] = number of edges shortest s-v path
        UUID[] nodesArray;  // array of nodes


        marked = new boolean[graph.getMyNumNodes()];
        distTo = new int[graph.getMyNumNodes()];
        edgeTo = new int[graph.getMyNumNodes()];

        ArrayList<UUID> nodesArrayList = dbManager.getKyes();
        nodesArrayList.remove(NUM_OF_EDGES);
        nodesArrayList.remove(NUM_OF_NODES);

        HashMap< Integer, ArrayList<UUID>> graphOrderedByDegree = new HashMap<>();

        if (graph.hasNode(s))
        {
            Queue<UUID> q = new java.util.ArrayDeque<UUID>();
            for (int i = 0; i < graph.getMyNumNodes(); i++)
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

                for (UUID w : graph.adjacentTo(v)) {
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