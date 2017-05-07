package com.relay.relay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.Util.GridSpacingItemDecoration;
import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.Imageutils;
import com.relay.relay.Util.LiveQueryMessageAdapter;
import com.relay.relay.Util.ShowActivityFullImage;
import com.relay.relay.system.RelayMessage;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.relay.relay.DB.InboxDB.REFRESH_INBOX_DB;
import static com.relay.relay.MainActivity.MESSAGE_RECEIVED;
import static com.relay.relay.Util.Imageutils.CAMERA_REQUEST;
import static com.relay.relay.Util.Imageutils.GALLERY_REQUEST;


public class ConversationActivity extends AppCompatActivity implements View.OnClickListener{

    private final String TAG = "RELAY_DEBUG: "+ ConversationActivity.class.getSimpleName();
    public static final int REFRESH_LIST_ADAPTER = 23;

    // helps to recognize if character is emoji
    private final String regex = "([\\u20a0-\\u32ff\\ud83c\\udc00-\\ud83d\\udeff\\udbb9\\udce5-\\udbb9\\udcee])";

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
    private ImageView mAttachment;
    private ImageView mDeleteAttachment;
    private RecyclerView mMessageRecyclerView;

    // contact list adapter
    private LiveQuery listsLiveQuery = null;
    private ListAdapter mAdapter;

    // Values and permissions adding picture
   // private Uri mLoadedImageUri;

    //For Image Attachment
    private Imageutils imageUtils;
    private Bitmap loadedBitmap;
    private Uri loadedUri;

    // Listener when new update arrived (new msg or update) to refresh database
    private BroadcastReceiver mBroadcastReceiver;
    private IntentFilter mFilter;

    public Messenger messenger =  new Messenger(new IncomingHandler());


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


        // enter to data base
        mDataManager = new DataManager(this);
        mDataBase = mDataManager.getInboxDB().getDatabase();

        mEditText = (EditText) findViewById(R.id.edit_text_write_message_area);
        mEditText.setCursorVisible(false);
        mEditText.setOnClickListener(this);

        mSendButton = (Button) findViewById(R.id.button_send_message_area);
        mSendButton.setOnClickListener(this);
        mAttachment = (ImageView) findViewById(R.id.imageView_attachment_send_message_area);
        mAttachment.setVisibility(View.GONE);
        mDeleteAttachment = (ImageView) findViewById(R.id.imageView_close_attachment_send_message_area);
        mDeleteAttachment.setOnClickListener(this);
        mDeleteAttachment.setVisibility(View.GONE);
        mAddPictureButton = (ImageView) findViewById(R.id.imageView_add_picture_send_message_area);
        mAddPictureButton.setOnClickListener(this);

        initMessageRecyclerView();

        imageUtils = new Imageutils(this);

        // start listen to connectivity Manager when there any updates with messages
        createBroadcastReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        unregisterReceiver(mBroadcastReceiver);
        super.onBackPressed();
    }


    /**
     * Hides the soft keyboard
     */
    public void hideSoftKeyboard() {
        if(getCurrentFocus()!=null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Shows the soft keyboard
     */
    public void showSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        view.requestFocus();
        inputMethodManager.showSoftInput(view, 0);
    }

    public void initMessageRecyclerView(){

        // init messages view
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        
        // set query
        setupLiveQuery(uuidString);
        mAdapter = new ListAdapter(this,listsLiveQuery,messenger);

        mMessageRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_messages);
        mMessageRecyclerView.setLayoutManager(linearLayoutManager);
        mMessageRecyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(15), true));
        mMessageRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mMessageRecyclerView.setAdapter(mAdapter);
        mMessageRecyclerView.setItemViewCacheSize(30);
        initSwipe();
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

            case R.id.edit_text_write_message_area:
                mEditText.setCursorVisible(true);
                break;

            case R.id.button_send_message_area:
                boolean didSend = sendMessage(uuidString);
                if (didSend){
                    mEditText.setCursorVisible(false);
                }
                break;

            case R.id.imageView_add_picture_send_message_area:
                imagepicker();
                break;

            case R.id.imageView_close_attachment_send_message_area:

                break;

        }
    }

    public void cropImage(Uri uri){
        if (uri != null){
            // crop image
            CropImage.activity(uri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        imageUtils.request_permission_result(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    loadedUri = result.getUri();
                    loadedBitmap = uriToBitmap(loadedUri);
                    setSmallImageInAttachment(loadedBitmap);
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
                break;

            case CAMERA_REQUEST:
                if(resultCode == RESULT_OK) {
                    loadedBitmap = (Bitmap) data.getExtras().get("data");
                    setSmallImageInAttachment(loadedBitmap);
                }
                break;

            case GALLERY_REQUEST:
                if(resultCode== RESULT_OK) {
                    Log.i("Gallery","Photo");
                    loadedUri = data.getData();
                    cropImage(loadedUri);
                }
                break;
        }

    }

    private boolean sendMessage(String destination) {

        String content = mEditText.getText().toString();
        //check if there is an image to upload
        if (mAttachment.getVisibility() == View.VISIBLE){

            // add relay message with attachment with image
            if (loadedBitmap != null) {
                RelayMessage newMessage = new RelayMessage(
                        mDataManager.getNodesDB().getMyNodeId(), UUID.fromString(destination), RelayMessage.TYPE_MESSAGE_INCLUDE_ATTACHMENT,
                        content,ImageConverter.ConvertBitmapToBytes(loadedBitmap));
                mDataManager.getMessagesDB().addMessage(newMessage);
                requestForSearch();
            }
        }
        else{
            if (content.trim().length() != 0) {
                RelayMessage newMessage = new RelayMessage(
                        mDataManager.getNodesDB().getMyNodeId(), UUID.fromString(destination), RelayMessage.TYPE_MESSAGE_TEXT,
                        content, null);
                mDataManager.getMessagesDB().addMessage(newMessage);
                requestForSearch();
            }
            else{
                return false;
            }

        }
        mEditText.setText("");
        mAttachment.setVisibility(View.GONE);
        mDeleteAttachment.setVisibility(View.GONE);
        return true;
    }


    public Bitmap uriToBitmap(Uri uri){

        Bitmap bitmap = null;
        try {
            bitmap =  MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public void imagepicker() {

        final CharSequence[] items;

        if(imageUtils.isDeviceSupportCamera()) {
            items=new CharSequence[2];
            items[0]="Camera";
            items[1]="Gallery";
        }
        else {
            items=new CharSequence[1];
            items[0]="Gallery";
        }

        android.app.AlertDialog.Builder alertdialog = new android.app.AlertDialog.Builder(this);
        alertdialog.setTitle("Add Image");
        alertdialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Camera")) {
                    imageUtils.launchCamera();
                }
                else if (items[item].equals("Gallery")) {
                    imageUtils.launchGallery();
                }
            }
        });
        alertdialog.show();
    }

    /**
     * When ending message, the activity will try ti initiate search from service to send immediately the new message.
     * it will work only if the service is in disconnected mode or wifi connected
     */
    public void requestForSearch(){
        //  BroadCast to service
        Intent updateActivity = new Intent(RelayConnectivityManager.SEARCH_FOR_HANDSHAKE_AFTER_ADDING_MESSAGE);
        activity.sendBroadcast(updateActivity);
    }

    public void setSmallImageInAttachment(Bitmap image){
        // set image in attachment
        // create small pic with low resolution
        Bitmap smallPic;
        smallPic =ImageConverter.scaleDown(image,300,true);
        smallPic = ImageConverter.getRoundedCornerBitmap(smallPic,10);
        mAttachment.setImageBitmap(smallPic);
        mAttachment.setVisibility(View.VISIBLE);
        mDeleteAttachment.setVisibility(View.VISIBLE);
    }


    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    // the live qeury will listen to the db changes, while the arraylist will be for the ui
    public void setupLiveQuery(final String uuid) {
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


        // set listsLiveQuery
        listsLiveQuery = query.toLiveQuery();
    }



    private class ListAdapter extends LiveQueryMessageAdapter {


        public ListAdapter(Context context, LiveQuery query,Messenger messenger) {
            super(context, query,messenger);
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_card, parent, false);

            return new MessageViewHolder(view);
        }



        @Override
        public void onBindViewHolder( final MessageViewHolder holder, final int position) {


            Map<String,Object> properties = enumerator.getRow(position).getDocument().getProperties();

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
                holder.cardAlignment.setLayoutParams(lp);

            }else{
                // set user name
                holder.fullName.setText(userName);
                holder.status.setVisibility(View.GONE);
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.messageBackground));
            }

            // todo disable update - do not delete
//            // set update message
//            if (update)
//                holder.updates.setVisibility(View.VISIBLE);
//            else
//                holder.updates.setVisibility(View.GONE);

            // set text message and picture
            if (relayMessage.getType() == relayMessage.TYPE_MESSAGE_TEXT){

                // if its emoji
                if (relayMessage.getContent().length() == 2){
                    //TODO check the char is emoji ascii

                    if (relayMessage.getContent().matches(regex))
                        holder.textMessage.setTextSize(40);
                }

                holder.textMessage.setText(relayMessage.getContent());
                holder.pictureAttachment.setVisibility(View.GONE);
            }
            else if (relayMessage.getType() == relayMessage.TYPE_MESSAGE_INCLUDE_ATTACHMENT){
                // if content is empty
                if (relayMessage.getContent().equals("")){
                    //holder.textMessage.setVisibility(View.GONE);
                    holder.textMessage.setText("");
                    holder.textMessage.setTextSize(0);
                }
                else
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

            // todo disable update - do not delete
//            // listener to card message
//            holder.cardAlignment.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    if (holder.updates.getVisibility() == View.VISIBLE)
//                        mDataManager.getInboxDB().setMessageSeenByUser((UUID.fromString(uuidString)));
//                }
//            });

            // listener to  picture
            holder.pictureAttachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new ShowActivityFullImage(ImageConverter.convertBytesToBitmap(relayMessage.getAttachment()),activity);
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
        // todo disable update - do not delete
      //  public ImageView updates;
        public ImageView status;
        public LinearLayout cardAlignment;
        public android.support.v7.widget.CardView cardView;


        public MessageViewHolder(View view) {
            super(view);

            cardAlignment = (LinearLayout) view.findViewById(R.id.message_card_view_alignment);
            fullName = (TextView) view.findViewById(R.id.textView_item_contact_name);
            textMessage = (TextView) view.findViewById(R.id.textView_item_contact_message);
            time = (TextView) view.findViewById(R.id.textView_item_message_time);
            pictureAttachment = (ImageView) view.findViewById(R.id.image_view_picture_attachment);
            // todo disable update - do not delete
            //updates = (ImageView) view.findViewById(R.id.imageView_item_message_updates);
            status = (ImageView) view.findViewById(R.id.imageView_item_message_status);
            cardView = (android.support.v7.widget.CardView ) view.findViewById(R.id.message_card);
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

    private void initSwipe(){

        final Paint p = new Paint();

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                //when I want to use direction
//                if (direction == ItemTouchHelper.LEFT){} else {}
                // set view to default alpha ( not impact other cards after delete )

                viewHolder.itemView.setAlpha((float)1.0);

                Document doc =  mAdapter.getItem(position);
                if(doc != null) {
                    Map<String, Object> properties = doc.getProperties();
                    String uuidString = (String) properties.get("uuid");
                    mDataManager.getInboxDB().deleteMessageFromInbox(UUID.fromString(uuidString));
                    Toast.makeText(activity, "Message deleted", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                Bitmap icon;
                if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
                    View itemView = viewHolder.itemView;
                    if(dX > 0){
                        itemView.setAlpha((float)(0.9-dX/500));
                    } else {
                        itemView.setAlpha((float)(0.9+dX/500));
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mMessageRecyclerView);
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_LIST_ADAPTER:
                    mMessageRecyclerView.setAdapter(mAdapter);
                    break;
            }
        }
    }


    private void refreshMessageRecyclerView(){
        mDataManager.getMessagesDB().getDatabase().close();
        mDataManager.getInboxDB().getDatabase().close();
        try {
            mDataManager.getInboxDB().getDatabase().open();
            mDataManager.getMessagesDB().getDatabase().open();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        setupLiveQuery(uuidString);
        mAdapter.myNotify();
    }

    /**
     * BroadcastReceiver
     */
    private  void createBroadcastReceiver() {
        mFilter = new IntentFilter();
        mFilter.addAction(REFRESH_INBOX_DB);
        mFilter.addAction(MESSAGE_RECEIVED);

        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                switch (action){
                    // When incoming message
                    case MESSAGE_RECEIVED:
                        mAdapter.myNotify();
                        break;

                    case REFRESH_INBOX_DB:
                        refreshMessageRecyclerView();
                        break;
                }
            }
        };
        registerReceiver(mBroadcastReceiver, mFilter);
    }
}
