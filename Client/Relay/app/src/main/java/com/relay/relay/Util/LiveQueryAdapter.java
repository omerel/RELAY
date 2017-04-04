package com.relay.relay.Util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.relay.relay.InboxFragment;
import com.relay.relay.MainActivity;
import com.relay.relay.R;

/**
 * Created by omer on 31/03/2017.
 */

public class LiveQueryAdapter extends RecyclerView.Adapter<InboxFragment.ContactViewHolder> {

    private LiveQuery query;
    public QueryEnumerator enumerator;
    private Context context;


    public LiveQueryAdapter(Context context, LiveQuery query) {
        this.context = context;
        this.query = query;

        query.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent event) {
                ((Activity) LiveQueryAdapter.this.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enumerator = event.getRows();
                        notifyDataSetChanged();
                    }
                });
            }
        });
        query.start();
    }

    @Override
    public InboxFragment.ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
       return null;
    }

    @Override
    public void onBindViewHolder(InboxFragment.ContactViewHolder holder, int position) {}

    @Override
    public int getItemCount() {
        return enumerator != null ? enumerator.getCount() : 0;
    }

    public Document getItem(int position) {
        return enumerator != null ? enumerator.getRow(position).getDocument(): null;
    }
}
