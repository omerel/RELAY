package com.relay.relay.DB;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by omer on 26/02/2017.
 */

public class Test {

    public Test(){

    }
    public void startTest(){
        final String TAG = "RELAY_DEBUG: "+ Test.class.getSimpleName();
        Graph graph = new Graph();
        UUID[] uuids = new UUID[15];

        for (int i = 0; i < 15; i++){
            uuids[i] =  UUID.randomUUID();
        }

        graph.hasNode(uuids[0]);
        Log.e(TAG, "graph.hasNode(uuids[0])-->"+graph.hasNode(uuids[0]));
        graph.addNode(uuids[0]);
        Log.e(TAG, "graph.addNode(uuids[0])");
        graph.hasNode(uuids[0]);
        Log.e(TAG, "graph.hasNode(uuids[0])-->"+graph.hasNode(uuids[0]));
        graph.addEdge(uuids[0],uuids[3]);
        Log.e(TAG, "graph.addEdge(uuids[0],uuids[3])---> OK");
        graph.addEdge(uuids[0],uuids[1]);
        Log.e(TAG, "graph.addEdge(uuids[0],uuids[1])---> OK");
        Log.e(TAG, "graph.getMyNumEdges()--->"+graph.getMyNumEdges());
        Log.e(TAG, "graph.getMyNumNodes()--->"+graph.getMyNumNodes());
        //////////////
        Log.e(TAG, "Creating full graph");
        for (int i = 0; i < 5; i++){
            graph.addEdge(uuids[0],uuids[i]);
        }
        for (int i = 5; i < 7; i++){
            graph.addEdge(uuids[1],uuids[i]);
        }
        for (int i = 7; i < 10; i++){
            graph.addEdge(uuids[2],uuids[i]);
        }
        for (int i = 10; i < 13; i++){
            graph.addEdge(uuids[3],uuids[i]);
        }
        for (int i = 13; i < 14; i++){
            graph.addEdge(uuids[4],uuids[i]);
        }
        for (int i = 13; i < 14; i++){
            graph.addEdge(uuids[7],uuids[i]);
        }
        for (int i = 14; i < 14; i++){
            graph.addEdge(uuids[13],uuids[i]);
        }
        for (int i = 10; i < 14; i++){
            graph.addEdge(uuids[5],uuids[i]);
        }
        graph.addEdge(uuids[10],uuids[14]);


        Log.e(TAG, "graph.getMyNumEdges()--->"+graph.getMyNumEdges());
        Log.e(TAG, "graph.getMyNumNodes()--->"+graph.getMyNumNodes());


        Log.e(TAG, "Start BFS on uuid[0]");
         HashMap< Integer, ArrayList<UUID>>  b = graph.bfs(graph, uuids[0]);

        Log.e(TAG, "Degrees :"+b.size());
        for (int i = 0; i< b.size();i++){
            Log.e(TAG, "Degree :"+i);
            ArrayList<UUID> arr = b.get(i);
            for (int j = 0; j < arr.size(); j++ ){
                Log.e(TAG, "Node :"+arr.get(j));
            }
        }
    }
}
