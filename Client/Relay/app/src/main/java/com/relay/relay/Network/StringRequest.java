package com.relay.relay.Network;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Created by omer on 05/06/2017.
 */

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class StringRequest extends Request<String> implements NetworkConstants{
    private final Listener<String> mListener;
   // private String mJson;
    private int mPutCommand;
    private Map<String, String>  mParams;

    /**
     * Creates a new request with the given method.
     *
     * @param method the request {@link Method} to use
     * @param url URL to fetch the string at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public StringRequest(int method, String url, Listener<String> listener,
                         ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
    }

    /**
     * Creates a new GET request.
     *
     * @param url URL to fetch the string at
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public StringRequest(String url, Listener<String> listener, ErrorListener errorListener) {
        this(Method.GET, url, listener, errorListener);
    }


     //Creates a new PUT request.

    public StringRequest(String url, Listener<String> listener, ErrorListener errorListener,
                         Map<String, String>  params ) {
        this(Method.PUT, url, listener, errorListener);
        this.mParams= params;
    }


    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String mJsonResponse;
        try {
            mJsonResponse = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            mJsonResponse = new String(response.data);
        }
        return Response.success(mJsonResponse, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }

}
