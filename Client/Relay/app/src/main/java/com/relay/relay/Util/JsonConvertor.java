package com.relay.relay.Util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.relay.relay.system.HandShakeHistory;
import com.relay.relay.system.Node;
import com.relay.relay.system.RelayMessage;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by omer on 12/02/2017.
 */

public class JsonConvertor {

    public static String createJsonWithCommand(int command,Object content){
        if ( content == null )
            return null;
        Object[] array = new Array[2];
        array[0] = command;
        array[1] = content;
        String jsonString = new Gson().toJson(array);
        return jsonString;
    }

    public static int getCommand(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Object[]>(){}.getType();
        Object[] array = gson.fromJson(jsonString, type);
        return (int)array[0];
    }

    public static Object getContent(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Object[]>(){}.getType();
        Object[] array = gson.fromJson(jsonString, type);
        return array[1];
    }

    public static String ConvertToJson(Object content){
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

}
