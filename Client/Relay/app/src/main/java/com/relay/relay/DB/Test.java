package com.relay.relay.DB;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.relay.relay.R;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.DataTransferred;
import com.relay.relay.system.HandShakeHistory;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by omer on 26/02/2017.
 */

public class Test {

    private Context context;
    final String TAG = "RELAY_DEBUG: "+ Test.class.getSimpleName();
    private GraphRelations graphRelations ;
    private NodesDB nodesDB;
    private MessagesDB messagesDB;
    private UUID myID;
    private DataManager dataManager;
    private HandShakeDB handShakeDB;
    private UUID[] uuids;

    public Test(Context context){
        this.context = context;
        dataManager = new DataManager(context);
        graphRelations = dataManager.getGraphRelations();
        nodesDB = dataManager.getNodesDB();
        messagesDB = dataManager.getMessagesDB();
        handShakeDB = dataManager.getHandShakeDB();
    }

    public int randomIndex(int min,int max){
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    public void createDB(){
        // create ids;
        uuids = new UUID[15];
        for (int i = 0; i < 15; i++){
            uuids[i] =  UUID.randomUUID();
        }

        this.myID = uuids[0];

        // create random dates
        Calendar[] dates = new Calendar[30];
        for (int i =0;i<30;i++){
            long offset = Timestamp.valueOf("2017-01-01 00:00:00").getTime();
            long end = Timestamp.valueOf("2017-03-03 00:00:00").getTime();
            long diff = end - offset + 1;
            Timestamp rand = new Timestamp(offset + (long)(Math.random() * diff));
            dates[i] = Calendar.getInstance();
            dates[i].setTime(rand);
        }

        // creates names
        String[] firstName = {"omer","dan","gil","bar","shlomi","dov","eric","rinat","rachael",
                "adi","bat","dana","michal","shoolamit","shir"};

        // creates Lastnames
        String[] lastName = {"inbar","elgrably","levy","cohen","cnaan","nof","meyshar","gilis",
                "fink","bartov","mizrachi","ben-zion","tamam","alomg","valder"};

        // creates emails
        String[] email = new String[lastName.length];
        for(int i=0;i<email.length;i++){
            email[i] = lastName[i]+"@gmail.com";
        }

        // creates phone numbers
        String[] phoneNumber = new String[15];
        for(int i=0;i<phoneNumber.length;i++){
            String num = "";
            for (int j =0 ;j<7;j++){
                num = num + Integer.toString(randomIndex(0,9));
            }
            phoneNumber[i] = "054-"+num;
        }

        // creates Usernames
        String[] userName = new String[15];
        for(int i=0;i<userName.length;i++){
            userName[i] = firstName[i]+Integer.toString(randomIndex(0,9))+Integer.toString(randomIndex(0,9));
        }

        // creates profile picture
        Bitmap pic = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.pic);


        String[] messages = {"Hi","how are you","Great","what are you doing tonight?",
        "Im feeling fine","do you want to meet us?","Helllllooooo","OMG","Miss u!","Its working!!"};


        Log.e(TAG, "add Nodes to nodeDB");
        for (int i = 0; i < 15; i++){

            nodesDB.addNode(new Node(uuids[i],dates[randomIndex(0,29)],dates[randomIndex(0,29)],
                    3,email[randomIndex(0,14)],phoneNumber[randomIndex(0,14)],userName[i],
                    firstName[randomIndex(0,14)]+" "+lastName[randomIndex(0,14)], pic,i));
        }

        Log.e(TAG, "add Messages to messagesDB");
        for(int i=0;i<30;i++){
            messagesDB.addMessage( new RelayMessage(uuids[randomIndex(0,14)],uuids[randomIndex(0,14)],
                    RelayMessage.TYPE_MESSAGE_TEXT,messages[randomIndex(0,9)]));
        }

        //////////////
        Log.e(TAG, "Creating full graphRelations");
        for (int i = 0; i < 5; i++){
            graphRelations.addEdge(uuids[0],uuids[i]);
        }
        for (int i = 5; i < 7; i++){
            graphRelations.addEdge(uuids[1],uuids[i]);
        }
        for (int i = 7; i < 10; i++){
            graphRelations.addEdge(uuids[2],uuids[i]);
        }
        for (int i = 10; i < 13; i++){
            graphRelations.addEdge(uuids[3],uuids[i]);
        }
        for (int i = 13; i < 14; i++){
            graphRelations.addEdge(uuids[4],uuids[i]);
        }

        for (int i = 13; i < 14; i++){
            graphRelations.addEdge(uuids[7],uuids[i]);
        }
        for (int i = 14; i < 14; i++){
            graphRelations.addEdge(uuids[13],uuids[i]);
        }
        for (int i = 10; i < 14; i++){
            graphRelations.addEdge(uuids[5],uuids[i]);
        }
        graphRelations.addEdge(uuids[10],uuids[14]);

    }


    public void deleteDB(){
        graphRelations.deleteGraph();
        nodesDB.deleteNodedb();
        messagesDB.deleteMessageDB();
    }

    public void startTest(){


        //createDB();

        Log.e(TAG, "graphRelations.getMyNumEdges()--->"+ graphRelations.getMyNumEdges());
        Log.e(TAG, "graphRelations.getMyNumNodes()--->"+ graphRelations.getMyNumNodes());
        Log.e(TAG, "nodesDB.getNumNodes()--->"+ nodesDB.getNumNodes());
        Log.e(TAG, "myID = nodesDB.getMyNodeId()--->"+ nodesDB.getMyNodeId());


        ArrayList<UUID> t;
        // When creating DB at start
       //nodesDB.setMyNodeId(uuids[0]);
        myID = nodesDB.getMyNodeId();

        Log.e(TAG, "Start BFS on uuid[0]");
        HashMap< Integer, ArrayList<UUID>>  b = graphRelations.bfs(graphRelations,myID);

        Log.e(TAG, "Degrees :"+b.size());
        for (int i = 0; i< b.size();i++){
            Log.e(TAG, "Degree :"+i);
            ArrayList<UUID> arr = b.get(i);
            for (int j = 0; j < arr.size(); j++ ){
                Log.e(TAG, "Node :"+arr.get(j));
            }
        }


//        Log.e(TAG, "Creating dataTransferredManager");
//        DataTransferred dataTransferredManager = new DataTransferred(
//                                                        graphRelations,nodesDB,messagesDB);
//        Log.e(TAG, "Creating createMetaData()");
//        DataTransferred.Metadata metadata = dataTransferredManager.createMetaData();
//
//        Log.e(TAG, "My node id is: "+metadata.getMyNode().getId().toString());
//        Log.e(TAG, "My name is is: "+metadata.getMyNode().getFullName());
//
//
//        Log.e(TAG, "checking metadata - KnownRelations ");
//        Map<UUID,DataTransferred.KnownRelations> map1 = metadata.getKnownRelationsList();
//        Set<UUID> set1 = map1.keySet();
//        for (UUID uuid : set1){
//            Log.e(TAG, "id: "+uuid+", degree: "+map1.get(uuid).getNodeDegree()+
//                    " , tmsp: "+map1.get(uuid).getTimeStampNodeDetails().getTime());
//        }
//
//        Log.e(TAG, "checking metadata - KnownMessage ");
//
//        Map<UUID,DataTransferred.KnownMessage> map2 = metadata.getKnownMessagesList();
//        Set<UUID> set2 = map2.keySet();
//        for (UUID uuid : set2){
//            Log.e(TAG, "id: "+uuid+", status: "+map2.get(uuid).getStatus());
//        }

//        Log.e(TAG, "checking Handshake History DB - adding  random handshakes ");
//        ArrayList<UUID> nodeList = nodesDB.getNodesIdList();
//        for(int i = 0; i < 50; i++){
//            UUID tempid = nodeList.get(randomIndex(1,nodeList.size()-1));
//            handShakeDB.addEventToHandShakeHistoryWith(tempid);
//            Log.e(TAG, "hand shake with "+tempid);
//        }


        Log.e(TAG, "checking Handshake History - picking one for example ");
         HandShakeHistory handShakeHistory =
                 handShakeDB.getHandShakeHistoryWith(UUID.fromString("ace7bea9-02a6-4add-b5a3-fe5fa0ca2dc2"));

        Log.e(TAG, "handShakeHistory.getmHandShakeCounter()-->"+handShakeHistory.getmHandShakeCounter());
        Log.e(TAG, "handShakeHistory.getmHandShakeRank()-->"+handShakeHistory.getmHandShakeRank());
//        Log.e(TAG, "adding events to arise the rank");
//        handShakeHistory.addEvent();
//        handShakeHistory.addEvent();
//        handShakeHistory.addEvent();
//        handShakeHistory.addEvent();
//        handShakeHistory.addEvent();
//        handShakeHistory.addEvent();
//        handShakeHistory.addEvent();
//        handShakeHistory.addEvent();
//        Log.e(TAG, "handShakeHistory.getmHandShakeCounter()-->"+handShakeHistory.getmHandShakeCounter());
//        Log.e(TAG, "handShakeHistory.getmHandShakeRank()-->"+handShakeHistory.getmHandShakeRank());

        Log.e(TAG, "get handshake events:");
        ArrayList<HandShakeHistory.HandShakeEvent> handShakeEvents = handShakeHistory.getmHandShakeEvents();
        for (HandShakeHistory.HandShakeEvent h : handShakeEvents){
            Log.e(TAG, "geo: "+h.getGeoLocation()+" ,  time:"+h.getTimeStamp().getTime());
        }
//
//        Log.e(TAG, "cleaning hand shake events before this moment");
//        handShakeHistory.cleanHandShakeEvents(0);
//
//        Log.e(TAG, "handshake events size: "+handShakeEvents.size());
//
//        Log.e(TAG, "get handshake events: need to be empty");
//        handShakeEvents = handShakeHistory.getmHandShakeEvents();
//        for (HandShakeHistory.HandShakeEvent h : handShakeEvents){
//            Log.e(TAG, "geo: "+h.getGeoLocation()+" ,  time:"+h.getTimeStamp().getTime());
//        }
//
//        Log.e(TAG, "HandShakeEventLog size: "+handShakeHistory.getmHandShakeEventLog().size());
//        Log.e(TAG, "clean HandShakeEventLog: "+handShakeHistory.clearHandShakeEventLog());
//        Log.e(TAG, "HandShakeEventLog size: "+handShakeHistory.getmHandShakeEventLog().size());
//
//
//        Log.e(TAG, "Update handShak history DB with  random node - "+
//        handShakeDB.updateHandShakeHistoryWith(UUID.fromString(
//                "bed3b22f-b65f-48a7-97d5-67b6a9e4a9f3"),handShakeHistory));
//
//        Log.e(TAG, "cleanHandShakeHistory: "+ dataManager.cleanHandShakeHistory(0));
//        Log.e(TAG, "clearHandShakeHistoryLog: "+ dataManager.clearHandShakeHistoryLog());
//
//        handShakeDB.deleteHandShakeDB();

        //deleteDB();



    }

}
