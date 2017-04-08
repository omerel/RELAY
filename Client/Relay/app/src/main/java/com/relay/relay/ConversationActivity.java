package com.relay.relay;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.GridSpacingItemDecoration;
import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.LiveQueryMessageAdapter;
import com.relay.relay.Util.SearchUser;
import com.relay.relay.Util.ShowDialogWithPicture;
import com.relay.relay.system.RelayMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ConversationActivity extends AppCompatActivity implements View.OnClickListener{

    private final String TAG = "RELAY_DEBUG: "+ ConversationActivity.class.getSimpleName();

    // activity for passing to other classes
    private Activity activity = this;

    // intent values
    private String uuidString;
    private String userName;

    // database
    private DataManager mDataManager;
    private Database mDataBase;

    //contacts view and adapter
    private EditText mEditText;
    private Button mSendButton;
    private ImageView mAddPictureButton;
    private RecyclerView mMessageRecyclerView;

    // contact list adapter
    private LiveQuery listsLiveQuery = null;
    private ArrayList<Map<String,Object>> arrayListProperties = null;
    private ListAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        userName = intent.getStringExtra(MainActivity.USER_NAME);
        uuidString = intent.getStringExtra(MainActivity.USER_UUID);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Relay to "+userName);


        arrayListProperties = new ArrayList<>();

        // enter to data base
        mDataManager = new DataManager(this);
        mDataBase = mDataManager.getInboxDB().getDatabase();

        // set query
        setupLiveQuery(uuidString);
        mAdapter = new ListAdapter(this,listsLiveQuery,arrayListProperties);


        mEditText = (EditText) findViewById(R.id.edit_text_write_message_area);
        mSendButton = (Button) findViewById(R.id.button_send_message_area);
        mAddPictureButton = (ImageView) findViewById(R.id.imageView_add_picture_send_message_area);

        // init messages view

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);

        mMessageRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_messages);
        mMessageRecyclerView.setLayoutManager(linearLayoutManager);
        mMessageRecyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(15), true));
        mMessageRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mMessageRecyclerView.setAdapter(mAdapter);
        mMessageRecyclerView.setItemViewCacheSize(10);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.button_send_message_area:
                break;

            case R.id.imageView_add_picture_send_message_area:
                break;

        }
    }

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    // the live qeury will listen to the db changes, while the arraylist will be for the ui
    private void setupLiveQuery(final String uuid) {
        if (mDataBase == null) {
            Log.e(TAG,"Error, mDataBase is null!");
            return;
        }
        com.couchbase.lite.View listsView = mDataBase.getView("list/messages_list_with_"+uuid);
        if (listsView.getMap() == null) {
            listsView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    String type = (String) document.get("type");
                    String contact_parent = (String) document.get("contact_parent");
                    if ("message".equals(type)  && contact_parent.equals(uuid)) {
                        emitter.emit(document.get("time"), null);
                    }
                }
            }, "1.0");
        }
        Log.e(TAG,"how many rows : "+listsView.getCurrentTotalRows());
        Query query = listsView.createQuery();
        query.setDescending(true);


        // set arrayListProperties
        QueryEnumerator result = null;
        try {
            result = query.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        for (Iterator<QueryRow> it = result; it.hasNext(); ) {
            QueryRow row = it.next();
            arrayListProperties.add(row.getDocument().getProperties());
        }

        // set listsLiveQuery
        listsLiveQuery = query.toLiveQuery();
    }



    private class ListAdapter extends LiveQueryMessageAdapter {


        public ListAdapter(Context context, LiveQuery query,ArrayList<Map<String,Object>> arrayListProperties) {
            super(context, query,arrayListProperties);
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_card, parent, false);

            return new MessageViewHolder(view);
        }


        @Override
        public void onBindViewHolder(final MessageViewHolder holder, final int position) {

            //Map<String,Object> properties = enumerator.getRow(position).getDocument().getProperties();
            Map<String,Object> properties = arrayListProperties.get(position);

            final String uuidString = (String) properties.get("uuid");
            boolean isMyMessage = (boolean) properties.get("is_my_message");
            boolean update = (boolean) properties.get("update");
            //boolean disappear = (boolean) properties.get("disappear");
            String time = (String) properties.get("time");
            time = convertTimeToReadableString(time);

            // get Message from messageDB
            final RelayMessage relayMessage = mDataManager.getMessagesDB().getMessage((UUID.fromString(uuidString)));

            // set date
            holder.time.setText(time);

            if (isMyMessage){
                // set user name
                holder.fullName.setText("Me");
                RelativeLayout.LayoutParams lp =
                        (RelativeLayout.LayoutParams) holder.cardAlignment.getLayoutParams();
                lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            }else{
                // set user name
                holder.fullName.setText(userName);
            }

            // set update message
            if (update)
                holder.updates.setVisibility(View.VISIBLE);
            else
                holder.updates.setVisibility(View.GONE);

            // set text message and picture
            if (relayMessage.getType() == relayMessage.TYPE_MESSAGE_TEXT){
                holder.textMessage.setText(relayMessage.getContent());
                holder.pictureAttachment.setVisibility(View.GONE);
            }
            else if (relayMessage.getType() == relayMessage.TYPE_MESSAGE_INCLUDE_ATTACHMENT){
                holder.textMessage.setText(relayMessage.getContent());
                // create small pic with low resolution
                Bitmap smallPic = ImageConverter.convertBytesToBitmap(relayMessage.getAttachment());
                smallPic =ImageConverter.scaleDown(smallPic,300,true);
                smallPic = ImageConverter.getRoundedCornerBitmap(smallPic,5);
                holder.pictureAttachment.setImageBitmap(smallPic);
            }

            int status = relayMessage.getStatus();

            if (status == relayMessage.STATUS_MESSAGE_CREATED){
                holder.status.setImageResource(R.drawable.ic_status_waiting);

            }else if (status == relayMessage.STATUS_MESSAGE_SENT){
                holder.status.setImageResource(R.drawable.ic_status_sent);
            }else if (status == relayMessage.STATUS_MESSAGE_DELIVERED){
                holder.status.setImageResource(R.drawable.ic_status_delivered);
            }if (status == relayMessage.STATUS_MESSAGE_RECEIVED_IN_SERVER){
                holder.status.setImageResource(R.drawable.ic_status_cloud);
            }

            // listener to card message
            holder.cardAlignment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDataManager.getInboxDB().setMessageSeenByUser((UUID.fromString(uuidString)));
                    // update holder and arraylist
                    holder.updates.setVisibility(View.GONE);
                    arrayListProperties.set(position,getItem(position).getProperties());
                }
            });

            // listener to  picture
            holder.pictureAttachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new ShowDialogWithPicture(ImageConverter.convertBytesToBitmap(relayMessage.getAttachment()),activity);
                }
            });
        }
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        // Views
        public TextView fullName;
        public TextView textMessage;
        public TextView time;
        public ImageView pictureAttachment;
        public ImageView updates;
        public ImageView status;
        public LinearLayout cardAlignment;


        public MessageViewHolder(View view) {
            super(view);

            cardAlignment = (LinearLayout) view.findViewById(R.id.message_card_view_alignment);
            fullName = (TextView) view.findViewById(R.id.textView_item_contact_name);
            textMessage = (TextView) view.findViewById(R.id.textView_item_contact_message);
            time = (TextView) view.findViewById(R.id.textView_item_message_time);
            pictureAttachment = (ImageView) view.findViewById(R.id.image_view_picture_attachment);
            updates = (ImageView) view.findViewById(R.id.imageView_item_message_updates);
            status = (ImageView) view.findViewById(R.id.imageView_item_message_status);
        }

    }

    private String convertTimeToReadableString(String time){
        String year = time.substring(0,4);
        String month = time.substring(4,6);
        String day = time.substring(6,8);
        String hour = time.substring(8,10);
        String min = time.substring(10,12);
        return day+"/"+month+"  "+hour+":"+min;
    }
}
