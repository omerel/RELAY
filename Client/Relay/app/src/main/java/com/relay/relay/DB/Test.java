package com.relay.relay.DB;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.relay.relay.R;
import com.relay.relay.Util.DataTransferred;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
    private UUID[] uuids;

    public Test(Context context){
        this.context = context;
        graphRelations = new GraphRelations(context);
        nodesDB = new NodesDB(context,graphRelations);
        messagesDB = new MessagesDB(context);

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
        Date[] dates = new Date[30];
        for (int i =0;i<30;i++){
            long offset = Timestamp.valueOf("2017-01-01 00:00:00").getTime();
            long end = Timestamp.valueOf("2017-05-05 00:00:00").getTime();
            long diff = end - offset + 1;
            Timestamp rand = new Timestamp(offset + (long)(Math.random() * diff));
            dates[i] = rand;
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

     //   createDB();

        Log.e(TAG, "graphRelations.getMyNumEdges()--->"+ graphRelations.getMyNumEdges());
        Log.e(TAG, "graphRelations.getMyNumNodes()--->"+ graphRelations.getMyNumNodes());
        Log.e(TAG, "nodesDB.getNumNodes()--->"+ nodesDB.getNumNodes());

        ArrayList<UUID> t;

//        myID = uuids[0];
        myID = nodesDB.getNodesIdList().get(0);

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


        Log.e(TAG, "Creating dataTransferredManager");
        DataTransferred dataTransferredManager = new DataTransferred((Node)nodesDB.getNode(myID),
                                                        graphRelations,nodesDB,messagesDB);
        Log.e(TAG, "Creating createMetaData()");
        DataTransferred.Metadata metadata = dataTransferredManager.createMetaData();

        Log.e(TAG, "My node id is: "+metadata.getMyNode().getId().toString());
        Log.e(TAG, "My name is is: "+metadata.getMyNode().getFullName());

        ArrayList<DataTransferred.KnownRelations> kn = metadata.getKnownRelationsList();
        for (int i =0 ;i<kn.size(); i++){
            Log.e(TAG, "id: "+kn.get(i).getNodeId()+", degree: "+kn.get(i).getNodeDegree()+
                    " , tmsp: "+kn.get(i).getTimeStampNodeDetails().toString());
        }

        ArrayList<DataTransferred.KnownMessage> km = metadata.getKnownMessagesList();

        for (int i =0 ;i<kn.size(); i++){
            Log.e(TAG, "id: "+km.get(i).getMessageId()+", status: "+km.get(i).getStatus());
        }



         DataTransferred.UpdateNodeAndRelations up =
                 dataTransferredManager.createUpdateNodeAndRelations();

        ArrayList<Node> nd = up.getNodeList();
        for (int i =0 ;i<nd.size(); i++){
            Log.e(TAG, "email: "+nd.get(i).getEmail());
        }

        ArrayList<DataTransferred.NodeRelations> nr = up.getRelationsList();
        for (int i =0 ;i<nd.size(); i++){
            DataTransferred.NodeRelations n = nr.get(i);
            Log.e(TAG, "for id : "+nr.get(i).getNodeId());
            ArrayList<UUID> ids = nr.get(i).getRelations();
            for (int j =0 ;j<ids.size(); j++){
                Log.e(TAG, "friend id : "+ids.get(j).toString());
            }
        }
        // deleteDB();
    }

}
