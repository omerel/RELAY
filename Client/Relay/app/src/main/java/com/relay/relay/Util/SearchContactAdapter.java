package com.relay.relay.Util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.relay.relay.R;

import java.util.ArrayList;

/**
 * Created by omer on 05/04/2017.
 */

public class SearchContactAdapter extends RecyclerView.Adapter<SearchContactAdapter.SearchViewHolder> implements Filterable {

    public ArrayList<SearchUser> mArrayList;
    public ArrayList<SearchUser> mFilteredList;


    public SearchContactAdapter(ArrayList<SearchUser> arrayList) {
        mArrayList = arrayList;
        mFilteredList = new ArrayList<>();
    }

    @Override
    public SearchContactAdapter.SearchViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return null;
    }

    @Override
    public void onBindViewHolder(SearchContactAdapter.SearchViewHolder viewHolder, int i) {}

    @Override
    public int getItemCount() {
        return mFilteredList.size();
    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {

                String charString = charSequence.toString();

                if (charString.isEmpty()) {
                    mFilteredList = new ArrayList<>();
                } else {

                    ArrayList<SearchUser> filteredList = new ArrayList<>();

                    for (SearchUser searchUser : mArrayList) {

                        if (searchUser.getFullName().toLowerCase().contains(charString) ||
                                searchUser.getUserName().toLowerCase().contains("@"+charString) ||
                                searchUser.getEmail().toLowerCase().contains(charString)) {
                            filteredList.add(searchUser);
                        }
                    }
                    mFilteredList = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mFilteredList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mFilteredList = (ArrayList<SearchUser>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public class SearchViewHolder extends RecyclerView.ViewHolder{
        public TextView contact;
        public ImageView arrow;
        public SearchViewHolder(View view) {
            super(view);
            contact = (TextView)view.findViewById(R.id.textView_item_contact_search_name);
            arrow = (ImageView)view.findViewById(R.id.arrow_item_contact_search_name);
        }
    }

}
