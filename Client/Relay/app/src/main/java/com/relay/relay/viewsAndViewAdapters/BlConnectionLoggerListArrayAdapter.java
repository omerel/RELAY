package com.relay.relay.viewsAndViewAdapters;

/**
 * Created by omer on 26/04/2017.
 */

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.relay.relay.R;

import java.util.List;

import static com.relay.relay.viewsAndViewAdapters.StatusBar.FLAG_ADVERTISEMENT;
import static com.relay.relay.viewsAndViewAdapters.StatusBar.FLAG_CONNECTING;
import static com.relay.relay.viewsAndViewAdapters.StatusBar.FLAG_ERROR;
import static com.relay.relay.viewsAndViewAdapters.StatusBar.FLAG_HANDSHAKE;
import static com.relay.relay.viewsAndViewAdapters.StatusBar.FLAG_NO_CHANGE;
import static com.relay.relay.viewsAndViewAdapters.StatusBar.FLAG_SEARCH;

public class BlConnectionLoggerListArrayAdapter extends RecyclerView.Adapter<BlConnectionLoggerListArrayAdapter.ViewHolder> {

    private final List<BluetoothConnectionLogger> list;
    private final Activity context;


    public BlConnectionLoggerListArrayAdapter(Activity context, List<BluetoothConnectionLogger> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        holder.msg.setText(list.get(position).getTimeWithMsg());
        final int code = list.get(position).getFlagCode();
        switch(code){
            case FLAG_ADVERTISEMENT:
                holder.flag.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_flag_advertise));
                break;
            case FLAG_SEARCH:
                holder.flag.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_flag_search));
                break;
            case FLAG_CONNECTING:
                holder.flag.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_flag_connecting));
                break;
            case FLAG_HANDSHAKE:
                holder.flag.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_flag_handshake));
                break;
            case FLAG_ERROR:
                holder.flag.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_flag_error));
                break;
            case FLAG_NO_CHANGE:
                holder.flag.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_flag_ble_info));
                break;
            default:
                holder.flag.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_flag_idle));
                break;
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public ImageView flag;
        public TextView msg;

        public ViewHolder(View view) {
            super(view);
            msg = (TextView) view.findViewById(R.id.log_msg);
            flag = (ImageView) view.findViewById(R.id.flag);
        }
    }
}