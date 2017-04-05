package com.relay.relay.Util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.relay.relay.R;

import java.util.ArrayList;

/**
 * Created by omer on 05/04/2017.
 */

public class SearchContactAdapter extends RecyclerView.Adapter<SearchContactAdapter.ViewHolder> implements Filterable {

    private ArrayList<SearchUser> mArrayList;
    private ArrayList<SearchUser> mFilteredList;

    public SearchContactAdapter(ArrayList<SearchUser> arrayList) {
        mArrayList = arrayList;
        mFilteredList = arrayList;
    }

    @Override
    public SearchContactAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_contact_search_result, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SearchContactAdapter.ViewHolder viewHolder, int i) {

        if (mFilteredList.get(i).getUserName() == ""){
            viewHolder.contact.setText(mFilteredList.get(i).getEmail());
        }
        else{
            viewHolder.contact.setText("@"+mFilteredList.get(i).getUserName()+", "+
                    mFilteredList.get(i).getFullName());
        }
    }

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
                    mFilteredList = mArrayList;
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

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView contact;
        public ViewHolder(View view) {
            super(view);
            contact = (TextView)view.findViewById(R.id.textView_item_contact_search_name);
        }
    }

}
