package com.relay.relay.Util;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.relay.relay.ConversationActivity;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by omer on 07/04/2017.
 */

public class LiveQueryMessageAdapter extends RecyclerView.Adapter<ConversationActivity.MessageViewHolder>{

    private LiveQuery query;
    public QueryEnumerator enumerator;
    private Context context;

    public LiveQueryMessageAdapter(Context context, LiveQuery query, final Messenger messenger) {
        this.context = context;
        this.query = query;

        query.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent event) {
                ((Activity) LiveQueryMessageAdapter.this.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enumerator = event.getRows();
                       // notifyDataSetChanged();
                        try {
                            messenger.send(Message.obtain(null, ConversationActivity.REFRESH_LIST_ADAPTER));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        query.start();
    }

    @Override
    public ConversationActivity.MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ConversationActivity.MessageViewHolder holder, int position) {}

    @Override
    public int getItemCount() {
        return enumerator != null ? enumerator.getCount() : 0;
    }

    public Document getItem(int position) {
        return enumerator != null ? enumerator.getRow(position).getDocument(): null;
    }

    public void deleteItem(int position){
        notifyItemRangeRemoved(0,getItemCount());
    }

}
