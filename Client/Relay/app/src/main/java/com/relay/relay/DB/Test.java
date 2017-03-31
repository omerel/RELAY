package com.relay.relay.DB;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.relay.relay.R;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.TimePerformance;
import com.relay.relay.Util.UuidGenerator;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by omer on 26/02/2017.
 */

public class Test {

    private Context context;
    final String TAG = "RELAY_DEBUG: "+ Test.class.getSimpleName();

    private final int  MAX = 30;

    private GraphRelations graphRelations ;
    private NodesDB nodesDB;
    private MessagesDB messagesDB;
    private UUID myID;
    private DataManager dataManager;
    private HandShakeDB handShakeDB;

    private UUID[] uuids = {UUID.fromString("5d14e165-514e-4989-8e32-ae4aaa7f9d72"),
            UUID.fromString("3ab38ece-e5fa-4fff-95bf-9b9d36e8e3ad"),
            UUID.fromString("61f5d9c1-10aa-46d9-9990-c098dee99e98"),
            UUID.fromString("41560e8f-62d7-40bd-bbe8-87332984e22d"),
            UUID.fromString("fa68c55f-e658-4e61-8f00-c5834054fa8f"),
            UUID.fromString("def4b3cc-7da0-4992-8e2b-2fc4ef4bac32"),
            UUID.fromString("84b23841-663e-45cf-9068-3302e2ea823a"),
            UUID.fromString("b4141a2c-9827-49f8-a749-dc43ca648a72"),
            UUID.fromString("0425fa29-86e7-42c3-a7ef-8fcb854be927"),
            UUID.fromString("b25fe2f8-b9f7-4762-a7e9-3fc8c16c82ac"),
            UUID.fromString("46707906-7dd2-4eec-9bb7-061722671143"),
            UUID.fromString("54cbd62e-faad-4ae4-9998-d8de0b4ef710")};

    private String[] fullNAme =  {"Omer","Barr","Rachael","Adi","Stav","Ido","Eric","Rinat","Dana",
            "Michal","Shlomit","Shir"};
    private String[] phoneNumber;
    private String[] email;
    private String[] userName;
    private Bitmap pic;
    private String[] messages = {"Hi","how are you","Great","what are you doing tonight?",
            "Im feeling fine","do you want to meet us?","Helllllooooo","OMG","Miss u!","Its working!!"};
    TimePerformance timePerformance = new TimePerformance();


    private Calendar[] dates;

    public Test(Context context){

        this.context = context;
        dataManager = new DataManager(context);
        graphRelations = dataManager.getGraphRelations();
        nodesDB = dataManager.getNodesDB();
        messagesDB = dataManager.getMessagesDB();
        handShakeDB = dataManager.getHandShakeDB();
    }
    // not include max
    public int randomIndex(int min,int max){
        return min + (int)(Math.random() * ((max - min)));
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
        String[] fullNAme = {"Omer","Barr","Rachael","Adi","Stav","Ido","Eric","Rinat","Dana",
                "Michal","Shlomit","Shir"};


        // creates emails
        String[] email = new String[fullNAme.length];
        for(int i=0;i<email.length;i++){
            email[i] = fullNAme[i]+"@gmail.com";
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
        for(int i=0;i<fullNAme.length;i++){
            userName[i] = fullNAme[i]+Integer.toString(randomIndex(0,9))+Integer.toString(randomIndex(0,9));
        }

        // creates profile picture
        Bitmap pic = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.pic);


        String[] messages = {"Hi","how are you","Great","what are you doing tonight?",
        "Im feeling fine","do you want to meet us?","Helllllooooo","OMG","Miss u!","Its working!!"};


        Log.e(TAG, "add Nodes to nodeDB");
        for (int i = 0; i < fullNAme.length; i++){

            nodesDB.addNode(new Node(uuids[i],dates[randomIndex(0,29)],dates[randomIndex(0,29)],
                    1,email[randomIndex(0,fullNAme.length-1)],phoneNumber[randomIndex(0,14)],userName[i],
                    fullNAme[i], pic,i));
        }

        Log.e(TAG, "add Messages to messagesDB");
        for(int i=0;i<30;i++){
            messagesDB.addMessage( new RelayMessage(uuids[randomIndex(0,fullNAme.length)],uuids[randomIndex(0,fullNAme.length)],
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

    /**
     * 0-"Omer",1-"Barr",2-"Rachael",3-"Adi","4-Stav","5-Ido",6-"Eric",7-"Rinat",8-"Dana",9-"Michal",10-"Shlomit",11-"Shir"
     */
    public void baseDB(){
        // create random dates
        dates = new Calendar[MAX];
        for (int i =0;i<MAX;i++){
            long offset = Timestamp.valueOf("2017-01-01 00:00:00").getTime();
            long end = Timestamp.valueOf("2017-03-03 00:00:00").getTime();
            long diff = end - offset + 1;
            Timestamp rand = new Timestamp(offset + (long)(Math.random() * diff));
            dates[i] = Calendar.getInstance();
            dates[i].setTime(rand);
        }

        // creates emails
        email = new String[fullNAme.length];
        for(int i=0;i<email.length;i++){
            email[i] = fullNAme[i]+"@gmail.com";
        }

        // creates phone numbers
        phoneNumber = new String[fullNAme.length];
        for(int i=0;i<phoneNumber.length;i++){
            String num = "";
            for (int j =0 ;j<7;j++){
                num = num + Integer.toString(randomIndex(0,9));
            }
            phoneNumber[i] = "054-"+num;
        }

        // creates Usernames
        userName = new String[fullNAme.length];
        for(int i=0;i<userName.length;i++){
            userName[i] = fullNAme[i]+Integer.toString(randomIndex(0,9))+Integer.toString(randomIndex(0,9));
        }

        // creates profile picture
        pic = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.pic);
    }

    /**
     * omer
     */
    public void createDB_1(){

        int numOFNodes = 1;

        // choose  what is the device
        this.myID = uuids[0];
        nodesDB.setMyNodeId(uuids[0]);

        baseDB();

        Log.e(TAG, "add Nodes to nodeDB");
        for (int i = 0; i < numOFNodes; i++) {
            nodesDB.addNode(new Node(uuids[0],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                    1,email[i],phoneNumber[i], userName[i], fullNAme[i], pic,i));
        }

//        Log.e(TAG, "add Messages to messagesDB");
//        for(int i=0;i<MAX;i++){
//            messagesDB.addMessage( new RelayMessage(uuids[randomIndex(0,1)],uuids[randomIndex(1,fullNAme.length)],
//                    RelayMessage.TYPE_MESSAGE_TEXT,messages[randomIndex(0,messages.length)]));
//        }

        Log.e(TAG, "Creating full graphRelations");
//        graphRelations.addEdge(uuids[0],uuids[i]);

    }

    /**
     * barr
     */
    public void createDB_2(){

        int numOFNodes = 1;

        // choose  what is the device
        this.myID = uuids[1];
        nodesDB.setMyNodeId(uuids[1]);

        baseDB();

        Log.e(TAG, "add Nodes to nodeDB");
            nodesDB.addNode(new Node(uuids[1],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                    1,email[1],phoneNumber[1], userName[1], fullNAme[1], pic,0));


//        Log.e(TAG, "add Messages to messagesDB");
//        for(int i=0;i<MAX;i++){
//            messagesDB.addMessage( new RelayMessage(uuids[randomIndex(1,2)],uuids[randomIndex(2,fullNAme.length)],
//                    RelayMessage.TYPE_MESSAGE_TEXT,messages[randomIndex(0,messages.length)]));
//        }

        Log.e(TAG, "Creating full graphRelations");
//        graphRelations.addEdge(uuids[0],uuids[i]);
    }


    /**
     * omer - rachael
     * rachael - stav
     */
    public void createDB_3(){

        int numOFNodes = 3;

        // choose  what is the device
        this.myID = uuids[0];
        nodesDB.setMyNodeId(uuids[0]);

        baseDB();

        Log.e(TAG, "add Nodes to nodeDB");
        // add omer
        nodesDB.addNode(new Node(uuids[0],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                    1,email[0],phoneNumber[0], userName[0], fullNAme[0], pic,0));
        // add racahel
        nodesDB.addNode(new Node(uuids[2],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                1,email[2],phoneNumber[2], userName[2], fullNAme[2], pic,0));
        //add stav
        nodesDB.addNode(new Node(uuids[4],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                1,email[4],phoneNumber[4], userName[4], fullNAme[4], pic,0));

//        Log.e(TAG, "add Messages to messagesDB");
//        for(int i=0;i<MAX;i++){
//            messagesDB.addMessage( new RelayMessage(uuids[randomIndex(0,numOFNodes)],uuids[randomIndex(0,numOFNodes)],
//                    RelayMessage.TYPE_MESSAGE_TEXT,messages[randomIndex(0,messages.length)]));
//        }

        Log.e(TAG, "Creating full graphRelations");
        graphRelations.addEdge(uuids[0],uuids[2]);
        graphRelations.addEdge(uuids[2],uuids[4]);

    }


    /**
     * omer - rachael, adi
     * adi- omer,barr
     * rachael - stav,omer
     * stav- rachael,ido
     * barr -adi
     * ido - stav
     */
    public void createDB_4(){

        int numOFNodes = 6;

        // choose  what is the device
        this.myID = uuids[0];
        nodesDB.setMyNodeId(uuids[0]);

        baseDB();

        Log.e(TAG, "add Nodes to nodeDB");
        for (int i = 0; i < numOFNodes; i++) {
            nodesDB.addNode(new Node(uuids[i],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                    1,email[i],phoneNumber[i], userName[i], fullNAme[i], pic,i));
        }

//        Log.e(TAG, "add Messages to messagesDB");
//        for(int i=0;i<MAX;i++){
//            messagesDB.addMessage( new RelayMessage(uuids[randomIndex(0,numOFNodes)],uuids[randomIndex(0,numOFNodes)],
//                    RelayMessage.TYPE_MESSAGE_TEXT,messages[randomIndex(0,messages.length)]));
//        }

        Log.e(TAG, "Creating full graphRelations");
        graphRelations.addEdge(uuids[0],uuids[2]);
        graphRelations.addEdge(uuids[0],uuids[3]);
        graphRelations.addEdge(uuids[3],uuids[1]);
        graphRelations.addEdge(uuids[2],uuids[4]);
        graphRelations.addEdge(uuids[4],uuids[5]);
    }

    /**
     * omer - barr
     * adi- barr
     * barr -adi,omer
     */
    public void createDB_5(){

        int numOFNodes = 3;

        // choose  what is the device
        this.myID = uuids[1];
        nodesDB.setMyNodeId(uuids[1]);

        baseDB();

        Log.e(TAG, "add Nodes to nodeDB");
        // add bar
        nodesDB.addNode(new Node(uuids[1],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                1,email[1],phoneNumber[1], userName[1], fullNAme[1], pic,0));

        //add omer
        nodesDB.addNode(new Node(uuids[0],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                1,email[0],phoneNumber[0], userName[0], fullNAme[0], pic,0));
        //add adi
        nodesDB.addNode(new Node(uuids[3],dates[randomIndex(0,MAX)],dates[randomIndex(0,MAX)],
                1,email[3],phoneNumber[3], userName[3], fullNAme[3], pic,0));

//        Log.e(TAG, "add Messages to messagesDB");
//        for(int i=0;i<MAX;i++){
//            messagesDB.addMessage( new RelayMessage(uuids[randomIndex(0,numOFNodes)],uuids[randomIndex(0,numOFNodes)],
//                    RelayMessage.TYPE_MESSAGE_TEXT,messages[randomIndex(0,messages.length)]));
//        }

        Log.e(TAG, "Creating full graphRelations");
        graphRelations.addEdge(uuids[0],uuids[1]);
        graphRelations.addEdge(uuids[1],uuids[3]);
    }

    public void deleteDB(){
        graphRelations.deleteGraph();
        nodesDB.deleteNodedb();
        messagesDB.deleteMessageDB();
    }


    public void startTest(){

        /////////start

        timePerformance.start();
        Log.e(TAG, "Create DB");
        //////////////
        //createDB_1();
        /////////////
        Log.e(TAG, "graphRelations.getMyNumEdges()--->"+ graphRelations.getMyNumEdges());
        Log.e(TAG, "graphRelations.getMyNumNodes()--->"+ graphRelations.getMyNumNodes());
        myID = nodesDB.getMyNodeId();
        Log.e(TAG, "myID = nodesDB.getMyNodeId()--->"+ nodesDB.getMyNodeId());

        Log.e(TAG, "Start BFS on myID");
        HashMap< Integer, ArrayList<UUID>>  b = graphRelations.bfs(graphRelations,myID);

        Log.e(TAG, " Sum Of Degrees :"+b.size());
        for (int i = 0; i< b.size();i++){
            Log.e(TAG, " On Degree :"+i);
            ArrayList<UUID> arr = b.get(i);
            for (int j = 0; j < arr.size(); j++ ){
                Log.e(TAG, "Node :"+ nodesDB.getNode(arr.get(j)).getFullName());
            }
        }
        Log.e(TAG,"BFS :"+ timePerformance.stop());

        // add message
        // if Im omer

        RelayMessage m = new RelayMessage(uuids[0],uuids[1],
                RelayMessage.TYPE_MESSAGE_INCLUDE_ATTACHMENT,"this msg with img");
        m.addAttachment(pic,RelayMessage.TYPE_ATTACHMENT_BITMAP);
        RelayMessage m1 = new RelayMessage(uuids[0],uuids[1],
                RelayMessage.TYPE_MESSAGE_INCLUDE_ATTACHMENT,"this msg with img");
        m.addAttachment(pic,RelayMessage.TYPE_ATTACHMENT_BITMAP);

       if (myID.equals(uuids[0])){
//           messagesDB.addMessage(m);
//           messagesDB.addMessage(m1);
//            messagesDB.addMessage( new RelayMessage(uuids[0],uuids[6],
//                    RelayMessage.TYPE_MESSAGE_TEXT,"this msg will be sent"));
//            messagesDB.addMessage( new RelayMessage(uuids[0],uuids[1],
//                    RelayMessage.TYPE_MESSAGE_TEXT,"this msg will be delivered "));
//            messagesDB.addMessage( new RelayMessage(uuids[6],uuids[7],
//                    RelayMessage.TYPE_MESSAGE_TEXT,"this will not be sent"));
        }
        else{
//           messagesDB.addMessage( new RelayMessage(uuids[2],uuids[3],
//                    RelayMessage.TYPE_MESSAGE_TEXT,"this msg will be sent"));
//            messagesDB.addMessage( new RelayMessage(uuids[2],uuids[3],
//                    RelayMessage.TYPE_MESSAGE_TEXT,"this msg will be sent"));
//            messagesDB.addMessage( new RelayMessage(uuids[1],uuids[0],
//                    RelayMessage.TYPE_MESSAGE_TEXT,"this msg will be delivered "));
//            messagesDB.addMessage( new RelayMessage(uuids[6],uuids[7],
//                    RelayMessage.TYPE_MESSAGE_TEXT,"this will not be sent"));
        }
        ///
        Log.e(TAG, "Show all messages");
        ArrayList<UUID> arrayList = messagesDB.getMessagesIdList();
        for (UUID uuid : arrayList){
            RelayMessage relayMessage = messagesDB.getMessage(uuid);

            String senderName = "";
            Node sender = nodesDB.getNode(relayMessage.getSenderId());
            if (sender == null)
                senderName = relayMessage.getSenderId().toString();
            else
                senderName =nodesDB.getNode(relayMessage.getSenderId()).getFullName();
            String destinationName="";
            Node destination = nodesDB.getNode(relayMessage.getDestinationId());
            if (destination == null)
                destinationName = relayMessage.getSenderId().toString();
            else
                destinationName = nodesDB.getNode(relayMessage.getDestinationId()).getFullName();

            Log.e(TAG, "FROM: "+senderName+
                    " TO : "+destinationName+
                    " STATUS: "+relayMessage.getStatus()+" CONTENT: "+relayMessage.getContent());

           // messagesDB.deleteMessage(uuid);
        }

        UuidGenerator generator = new UuidGenerator();

        try {
            UUID uuid = generator.GenerateUUIDFromEmail("rachael83vasd@gmail.com");
            generator.GenerateEmailFromUUID(uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
