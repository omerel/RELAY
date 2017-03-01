package com.relay.relay.DB;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.relay.relay.R;
import com.relay.relay.system.Node;

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

    public Test(Context context){
        this.context = context;
    }

    public int randomIndex(int min,int max){
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    public void startTest(){
        final String TAG = "RELAY_DEBUG: "+ Test.class.getSimpleName();
        GraphRelations graphRelations = new GraphRelations(context);
        NodesDB nodesDB = new NodesDB(context,graphRelations);
        MessagesDB messagesDB = new MessagesDB(context);


        // create ids;
        UUID[] uuids = new UUID[15];
        for (int i = 0; i < 15; i++){
            uuids[i] =  UUID.randomUUID();
        }

        // create random dates
        Date[] dates = new Date[30];
        for (int i =0;i<30;i++){
            long offset = Timestamp.valueOf("2017-01-01 00:00:00").getTime();
            long end = Timestamp.valueOf("2017-05-05 00:00:00").getTime();
            long diff = end - offset + 1;
            Timestamp rand = new Timestamp(offset + (long)(Math.random() * diff));
            dates[i] = rand;
        }

        String[] firstName = {"omer","dan","gil","bar","shlomi","dov","eric","rinat","rachael",
        "adi","bat","dana","michal","shoolamit","shir"};

        String[] lastName = {"inbar","elgrably","levy","cohen","cnaan","nof","meyshar","gilis",
                "fink","bartov","mizrachi","ben-zion","tamam","alomg","valder"};

        String[] email = new String[lastName.length];
        for(int i=0;i<email.length;i++){
            email[i] = lastName[i]+"@gmail.com";
        }

        String[] phoneNumber = new String[15];
        for(int i=0;i<phoneNumber.length;i++){
            String num = "";
            for (int j =0 ;j<7;j++){
                num = num + Integer.toString(randomIndex(0,9));
            }
            phoneNumber[i] = "054-"+num;
        }

        String[] userName = new String[15];
        for(int i=0;i<userName.length;i++){
            userName[i] = firstName[i]+Integer.toString(randomIndex(0,9))+Integer.toString(randomIndex(0,9));
        }

        Bitmap[] pic = new Bitmap[15];
        for(int i=0;i<pic.length;i++){
            pic[i] = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.pic);
        }

//        public Node(UUID mId, Date mTimeStampNodeDetails, Date mTimeStampNodeRelations, int mRank,
//        String mEmail, String mPhoneNumber, String mUserName, String mFullName,
//                Bitmap mProfilePicture, int mResidenceCode) {


        Log.e(TAG, "add Nodes to nodeDB");
        for (int i = 0; i < 15; i++){
            nodesDB.addNode(new Node(uuids[i],dates[randomIndex(0,29)],dates[randomIndex(0,29)],
                    3,email[randomIndex(0,14)],phoneNumber[randomIndex(0,14)],userName[i],
            firstName[randomIndex(0,14)]+" "+lastName[randomIndex(0,14)],pic[i],i));
        }


        Log.e(TAG, "add messages to nodeDB");
        for (int i = 0; i < 15; i++){
         
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

        Log.e(TAG, "graphRelations.getMyNumEdges()--->"+ graphRelations.getMyNumEdges());
        Log.e(TAG, "graphRelations.getMyNumNodes()--->"+ graphRelations.getMyNumNodes());

        ArrayList<UUID> t;
        for (int i =0 ;i<15;i++){
           t =  (ArrayList<UUID>) graphRelations.adjacentTo(uuids[i]);
            Log.e(TAG, "num of adj for node "+i+": "+ (t.size()));
        }

        Log.e(TAG, "delete uuid[3]: "+ graphRelations.deleteNode(uuids[3]));
        Log.e(TAG, "Start BFS on uuid[0]");
        HashMap< Integer, ArrayList<UUID>>  b = graphRelations.bfs(graphRelations, uuids[0]);

        Log.e(TAG, "Degrees :"+b.size());
        for (int i = 0; i< b.size();i++){
            Log.e(TAG, "Degree :"+i);
            ArrayList<UUID> arr = b.get(i);
            for (int j = 0; j < arr.size(); j++ ){
                Log.e(TAG, "Node :"+arr.get(j));
            }
        }

        graphRelations.deleteGraph();
        nodesDB.deleteNodedb();
    }

}
