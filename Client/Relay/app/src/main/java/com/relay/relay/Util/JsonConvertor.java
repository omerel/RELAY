package com.relay.relay.Util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

/**
 * Created by omer on 12/02/2017.
 */

public class JsonConvertor {

    public String createJsonWithCommand(int command,Object content){
        if ( content == null )
            return null;
        Object[] array = new Array[2];
        array[0] = command;
        array[1] = content;
        String jsonString = new Gson().toJson(array);
        return jsonString;
    }

    public int getCommand(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Object[]>(){}.getType();
        Object[] array = gson.fromJson(jsonString, type);
        return (int)array[0];
    }

    public Object getContent(String jsonString){
        Gson gson = new Gson();
        Type type = new TypeToken<Object[]>(){}.getType();
        Object[] array = gson.fromJson(jsonString, type);
        return array[1];
    }

}
