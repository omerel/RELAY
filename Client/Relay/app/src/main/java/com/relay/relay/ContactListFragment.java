package com.relay.relay;

/**
 * Created by omer on 21/01/2017.
 */


import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Visibility;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.app.Activity;

import static com.relay.relay.MainActivity.mId;
import static com.relay.relay.MainActivity.mMapMessages;


public class ContactListFragment extends Fragment{


    private static final String ARGUMENT_ME= "me";

    private int[] mImageResIds;
    private String[] mNames;
    public static int[] messagesCounter = new int[4];
    private String me;

    private OnContactSelected mListener;
    private ContactAdapter contactAdapter;

    public static ContactListFragment newInstance(String sender) {

        final Bundle args = new Bundle();
        args.putString(ARGUMENT_ME, sender);
        final ContactListFragment fragment = new ContactListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public ContactListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final Bundle args = getArguments();
        me = args.getString(ARGUMENT_ME);

        if (context instanceof OnContactSelected) {
            mListener = (OnContactSelected) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement");
        }

        // Get profile and name - NICE WAY
//        final Resources resources = context.getResources();
//        mNames = resources.getStringArray(R.array.names);
        mNames = new String[3];
        mImageResIds = new int[mNames.length];
        int j =0;
        for (int i=0;i<4;i++){
            if(!mId[i].equals(me)){
                mNames[j] = mId[i];

                switch (mId[i]){
                    case "Ariel":
                        mImageResIds[j]= R.drawable.ariel;
                        break;
                    case "Omer":
                        mImageResIds[j]= R.drawable.omer;
                        break;
                    case "Barr":
                        mImageResIds[j]= R.drawable.barr;
                        break;
                    case "Boris":
                        mImageResIds[j]= R.drawable.boris;
                        break;
                }
                j++;
            }
        }


//        // Get profile images.- NICE WAY
//        final TypedArray typedArray = resources.obtainTypedArray(R.array.images);
//        final int imageCount = mNames.length ;
//        mImageResIds = new int[imageCount];
//        for (int i = 0; i < imageCount; i++) {
//            mImageResIds[i] = typedArray.getResourceId(i, 0);
//        }
//        typedArray.recycle();

        // init counter to know if there are ne messages;
        messagesCounter[0] = 0;
        messagesCounter[1] = 0;
        messagesCounter[2] = 0;
        messagesCounter[3] = 0;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.contact_list, container, false);

        final Activity activity = getActivity();
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        contactAdapter = new ContactAdapter(activity);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));// GridLayoutManager(activity, 2));
        recyclerView.setAdapter(contactAdapter);
        return view;
    }

    public void notifyDataSetChanged() {
        contactAdapter.notifyDataSetChanged();
    }


    class ContactAdapter extends RecyclerView.Adapter<ViewHolder> {

        private LayoutInflater mLayoutInflater;

        public ContactAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            return new ViewHolder(mLayoutInflater
                    .inflate(R.layout.chat_item, viewGroup, false));
        }

        @Override
        public void onBindViewHolder( ViewHolder viewHolder, final int position) {
            final int imageResId = mImageResIds[position];
            final String name = mNames[position];


            if (mMapMessages.get(name).size() != messagesCounter[position]) {
                viewHolder.setData(imageResId, name, true);
                messagesCounter[position] = mMapMessages.get(name).size();
            }
            else
                viewHolder.setData(imageResId, name, false);

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.OnContactSelected(name);
                }
            });

        }

        @Override
        public int getItemCount() {
            return mNames.length;
        }


    }

    class ViewHolder extends RecyclerView.ViewHolder {
        // Views
        private ImageView mImageView;
        private TextView mNameTextView;
        private TextView mstatus;

        private ViewHolder(View itemView) {
            super(itemView);

            // Get references to image and name.
            mImageView = (ImageView) itemView.findViewById(R.id.contactImage);
            mNameTextView = (TextView) itemView.findViewById(R.id.smReceivers);
            mstatus = (TextView)itemView.findViewById(R.id.message);
        }

        private void setData(int imageResId, String name ,boolean newMsg ) {
                mImageView.setImageResource(imageResId);
                mNameTextView.setText(name);
                if(newMsg)
                    mstatus.setText("New message");
                else
                    mstatus.setText("");
        }
    }

    public interface OnContactSelected {
        void OnContactSelected( String contact);
    }
}
