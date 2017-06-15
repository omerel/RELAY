package com.relay.relay.Util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.relay.relay.system.HandShakeHistory;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by omer on 12/02/2017.
 */

public class JsonConvertor {

    final static int COMMAND = 1;
    final static int CONTENT = 2;

    public static String createJsonWithCommand(int command,String jsonContent){
//        if ( jsonContent == null )
//            return null;
        Map<Integer,String> map = new HashMap<>();
        map.put(COMMAND,String.valueOf(command));
        map.put(CONTENT,jsonContent);
        String jsonString = convertToJson(map);
        isJSONValid(jsonString);
        return  jsonString;
    }

    public static int getCommand(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Map<Integer,String>>(){}.getType();
        Map<Integer,String> map = gson.fromJson(jsonString, type);
        return Integer.valueOf(map.get(COMMAND));
    }

    public static String getJsonContent(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Map<Integer,String>>(){}.getType();
        Map<Integer,String> map = gson.fromJson(jsonString, type);
        return map.get(CONTENT);
    }

    public static Map<String,String> getJsonBody(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String,String>>(){}.getType();
        Map<String,String> map = gson.fromJson(jsonString, type);
        return map;
    }

    public static DataTransferred.Metadata getMetadataFromJsonContent(String jsonString){
        String jsonMetadata = getJsonContent(jsonString);
        Gson gson = new Gson();
        Type type = new TypeToken<DataTransferred.Metadata>(){}.getType();
        return gson.fromJson(jsonMetadata, type);
    }

    public static RelayMessage getRelayMessageFromJsonContent(String jsonString){
        String jsonRelayMessage = getJsonContent(jsonString);
        Gson gson = new Gson();
        Type type = new TypeToken<RelayMessage>(){}.getType();
        return gson.fromJson(jsonRelayMessage, type);
    }

    public static DataTransferred.UpdateNodeAndRelations getUpdateNodeAndRelationsFromJsonContent(String jsonString){
        String jsonUpdateNodeAndRelations = getJsonContent(jsonString);
        Gson gson = new Gson();
        Type type = new TypeToken<DataTransferred.UpdateNodeAndRelations>(){}.getType();
        return gson.fromJson(jsonUpdateNodeAndRelations, type);
    }

    public static ArrayList<RelayMessage> getRelayMessageListFromJsonContent(String jsonString){
        String jsonRelayMessageList= getJsonContent(jsonString);
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<RelayMessage>>(){}.getType();
        return gson.fromJson(jsonRelayMessageList, type);
    }

    public static String convertToJson(Object content){
        if ( content == null )
            return null;
        String jsonString = new Gson().toJson(content);
        return jsonString;
    }

    public static Node JsonToNode(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Node>(){}.getType();
        return gson.fromJson(jsonString, type);
    }

    public static ArrayList<UUID> JsonToUUIDArrayList(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<UUID>>(){}.getType();
        return gson.fromJson(jsonString, type);
    }

    public static Map<UUID,DataTransferred.KnownRelations> getKnownRelationsFromJsonContent(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Map<UUID,DataTransferred.KnownRelations>>(){}.getType();
        return gson.fromJson(jsonString, type);
    }

    public static Map<UUID,DataTransferred.KnownMessage> getKnownMessageFromJsonContent(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Map<UUID,DataTransferred.KnownMessage>>(){}.getType();
        return gson.fromJson(jsonString, type);
    }

    public static Integer JsonToInt(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Integer>(){}.getType();
        return gson.fromJson(jsonString, type);
    }

    public static RelayMessage JsonToRelayMessage( String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<RelayMessage>(){}.getType();
        return gson.fromJson(jsonString, type);
    }

    public static HandShakeHistory JsonToHandShakeHistory(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<HandShakeHistory>(){}.getType();
        return gson.fromJson(jsonString, type);
    }

    public static UUID JsonToUUID(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<UUID>(){}.getType();
        return gson.fromJson(jsonString, type);
    }


    public static boolean isJSONValid(String jsonContent) {
        try {
            getCommand(jsonContent);
            getJsonContent(jsonContent);
        } catch (Exception ex) {
            String TAG = "RELAY_DEBUG: "+ JsonConvertor.class.getSimpleName();
            Log.e(TAG,"Error in JsonConvertor: \n ex.getMessage()");
        }
        return true;
    }
}
