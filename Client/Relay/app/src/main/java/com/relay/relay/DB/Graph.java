package com.relay.relay.DB;
import java.util.*;
import java.util.Queue;

public class Graph {
    private HashMap<UUID, TreeSet<UUID>> myGraph;
    private static final TreeSet<UUID> EMPTY_SET = new TreeSet<UUID>();
    private int myNumNodes;
    private int myNumEdges;

    /**
     * Construct empty Graph
     */
    public Graph() {
        myGraph = new HashMap<UUID, TreeSet<UUID>>();
        myNumNodes = myNumEdges = 0;
    }

    /**
     * Add a new node with no neighbors
     */
    public boolean addNode(UUID uuid) {
        final UUID node = uuid;
        if (!myGraph.keySet().contains(node)) {
            myGraph.put(node, new TreeSet<UUID>());
            myNumNodes += 1;
            return true;
        }
        return false;
    }

    /**
     * Returns true iff v is in this Graph, false otherwise
     */
    public boolean hasNode(UUID uuid) {
        return myGraph.keySet().contains(uuid);
    }

    /**
     * Is from-to, an edge in this Graph. The graph is
     * undirected so the order of from and to does not
     * matter.
     */
    public boolean hasEdge(UUID from, UUID to) {

        if (!hasNode(from) || !hasNode(to))
            return false;
        return myGraph.get(from).contains(to);
    }

    /**
     * Add to to from's set of neighbors, and add from to to's
     * set of neighbors. Does not add an edge if another edge
     * already exists
     */
    public void addEdge(UUID from, UUID to) {
        if (hasEdge(from, to))
            return;
        if (from == to)
            return;
        myNumEdges += 1;
        addNode(from); // TODO noo need because hasEdge already check it
        addNode(to);
        myGraph.get(from).add(to);
        myGraph.get(to).add(from);
    }


    /**
     * Return an iterator over the neighbors of UUID
     */
    public Iterable<UUID> adjacentTo(UUID uuid) {
        if (!myGraph.containsKey(uuid))
            return EMPTY_SET;
        return myGraph.get(uuid);
    }


    public int getMyNumNodes() {
        return myNumNodes;
    }

    public int getMyNumEdges() {
        return myNumEdges;
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
        ArrayList<UUID> nodesArrayList  = new ArrayList<UUID>(graph.myGraph.keySet());
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
//                        // if degree exist? add it to the graphOrderedByDegree
//                        if (graphOrderedByDegree.containsKey(distTo[nodesArrayList.indexOf(w)]))
//                        {
//                            ArrayList<UUID> tempList = graphOrderedByDegree.get(distTo[nodesArrayList.indexOf(w)]);
//                            tempList.add(w);
//                            graphOrderedByDegree.put(distTo[nodesArrayList.indexOf(w)],tempList);
//                        }
//                        else{
//                            ArrayList<UUID> tempList = new ArrayList<>();
//                            tempList.add(w);
//                            graphOrderedByDegree.put(distTo[nodesArrayList.indexOf(w)],tempList);
//                        }
                    }
                }
            }
        }
        return graphOrderedByDegree;
    }
}