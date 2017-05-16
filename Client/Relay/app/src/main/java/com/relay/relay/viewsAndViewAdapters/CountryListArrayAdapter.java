package com.relay.relay.viewsAndViewAdapters;

/**
 * Created by omer on 26/04/2017.
 */

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.relay.relay.R;

import java.util.List;

public class CountryListArrayAdapter extends ArrayAdapter<String> {

    private final List<String> list;
    private final Activity context;

    static class ViewHolder {
        protected TextView name;
    }

    public CountryListArrayAdapter(Activity context, List<String> list) {
        super(context, R.layout.item_text, list);
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;

        if (convertView == null) {
            LayoutInflater inflator = context.getLayoutInflater();
            view = inflator.inflate(R.layout.item_text, null);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.name = (TextView) view.findViewById(R.id.textView_simpleText);
            view.setTag(viewHolder);
        } else {
            view = convertView;
        }

        ViewHolder holder = (ViewHolder) view.getTag();
        holder.name.setText(list.get(position));
        return view;
    }
}