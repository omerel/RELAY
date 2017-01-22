package com.relay.relay;

/**
 * Created by omer on 21/01/2017.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.app.Activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;


import static com.relay.relay.ContactListFragment.messagesCounter;
import static com.relay.relay.MainActivity.A;
import static com.relay.relay.MainActivity.B;
import static com.relay.relay.MainActivity.C;
import static com.relay.relay.MainActivity.D;
import static com.relay.relay.MainActivity.DELIMITER;
import static com.relay.relay.MainActivity.db;
import static com.relay.relay.MainActivity.mMapMessages;

public class ChatFragment extends Fragment {

    public ChatAdapter chatAdapter;
    private static final String ARGUMENT_ME= "me";
    private static final String ARGUMENT_CONTACT = "contact";
    private Button mSendButton;
    private EditText mContent;


    public static ChatFragment newInstance(String me,String contact) {
        final Bundle args = new Bundle();
        args.putString(ARGUMENT_ME, me);
        args.putString(ARGUMENT_CONTACT, contact);
        final ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        final Bundle args = getArguments();
        final String me = args.getString(ARGUMENT_ME);
        final String contact = args.getString(ARGUMENT_CONTACT);

        final View view = inflater.inflate(R.layout.chat_list, container, false);
        final Activity activity = getActivity();
        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_chat);

        chatAdapter = new ChatAdapter(activity,me,contact);


        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        recyclerView.setAdapter(chatAdapter);

        mSendButton = (Button) view.findViewById(R.id.send_button);
        mContent = (EditText) view.findViewById(R.id.content);

        mSendButton.setClickable(false);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String content = mContent.getText().toString();
                mContent.setText("");
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");

                if (!content.isEmpty()) {
                    // update dialog
                    MainActivity.RelayMesage newMessage;
                    String time = df.format(Calendar.getInstance().getTime());
                    newMessage = new MainActivity.RelayMesage(me, contact, content, null, time);
                    mMapMessages.get(contact).add(newMessage);

                    // update db for the next sync
                    Object[] obj = new Object[2];
                    obj[0] = df.format(Calendar.getInstance().getTime());
                    obj[1] = me + DELIMITER + content;
                    db.put(contact, obj);
                }
            }
        });
        return view;
    }


    public void notifyDataSetChanged(){
        chatAdapter.notifyDataSetChanged();
        //mArrayMessages.setSelection(mChatAdapter.getCount()-1);
    }

    class ChatAdapter extends RecyclerView.Adapter<ViewHolder> {

        private LayoutInflater mLayoutInflater;
        private String me;
        private String contact;
        private Context context;

        public ChatAdapter(Context context,String me, String contact) {
            mLayoutInflater = LayoutInflater.from(context);
            this.me = me;
            this.contact = contact;
            this.context = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            return new ViewHolder(mLayoutInflater
                    .inflate(R.layout.chat_item, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {

            ArrayList<MainActivity.RelayMesage> mArrayMessages;

            mArrayMessages =  mMapMessages.get(contact);
            MainActivity.RelayMesage msg = mArrayMessages.get(position);

            boolean myMsg = me.equals(msg.sender);
            if (myMsg){
                viewHolder.setData(context,"Me",msg);
            }
            else{
                viewHolder.setData(context,contact,msg);
            }
        }

        @Override
        public int getItemCount() {
            return mMapMessages.get(contact).size();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        // Views
        public TextView txtMessage;
        public TextView txtTime;
        public TextView userName;
        public ImageView image;
        public ImageView profile;

        private ViewHolder(View itemView) {
            super(itemView);

            txtMessage = (TextView) itemView.findViewById(R.id.message);
            txtTime = (TextView) itemView.findViewById(R.id.createdAtTime);
            profile = (ImageView)itemView.findViewById(R.id.contactImage);
            image = (ImageView)itemView.findViewById(R.id.image_message);
            userName = (TextView)itemView.findViewById(R.id.smReceivers);
        }

        private void setData(Context context, String title, MainActivity.RelayMesage msg) {

            userName.setText(title);
            if (msg.textMsg != null )
                txtMessage.setText(msg.textMsg);
            if (msg.image != null )
                image.setImageBitmap(getBitmapFromBytes(msg.image.getBytes()));
            txtTime.setText( msg.time );

            switch (msg.sender){
                case "Ariel":
                    profile.setImageDrawable(context.getDrawable(R.drawable.ariel));
                    break;
                case "Omer":
                    profile.setImageDrawable(context.getDrawable(R.drawable.omer));
                    break;
                case "Barr":
                    profile.setImageDrawable(context.getDrawable(R.drawable.barr));
                    break;
                case "Boris":
                    profile.setImageDrawable(context.getDrawable(R.drawable.boris));
                    break;
            }
        }

        public Bitmap getBitmapFromBytes(byte[] byteArray){
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            return bitmap;
        }
    }

}
