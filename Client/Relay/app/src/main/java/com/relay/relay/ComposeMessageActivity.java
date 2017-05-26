package com.relay.relay;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.SubSystem.RelayConnectivityManager;
import com.relay.relay.Util.GridSpacingItemDecoration;
import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.ImagePicker;
import com.relay.relay.viewsAndViewAdapters.SearchContactAdapter;
import com.relay.relay.Util.SearchUser;
import com.relay.relay.Util.UuidGenerator;
import com.relay.relay.system.RelayMessage;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static com.relay.relay.Util.ImagePicker.CAMERA_REQUEST;
import static com.relay.relay.Util.ImagePicker.GALLERY_REQUEST;

public class ComposeMessageActivity extends AppCompatActivity implements View.OnClickListener {

    // activity for passing to other classes
    private Activity activity = this;

    // database
    private DataManager mDataManager;

    //contacts view and adapter
    private EditText mEditTextSendTo;
    private EditText mEditTextSubject;
    private EditText mEditTextContent;
    private ImageView mAttachment;
    private ImageView mDeleteAttachment;

    // search view and adapter
    private RecyclerView mSearchContactRecyclerView;
    private ArrayList<SearchUser> mSearchContactArrayList;
    private SearchListAdapter mSearchListAdapter;

    //For Image Attachment
    private ImagePicker imageUtils;
    private Bitmap loadedBitmap;
    private Uri loadedUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Compose message");

        // enter to data base
        mDataManager = new DataManager(this);

        mEditTextSendTo = (EditText) findViewById(R.id.compose_editText_email);
        mEditTextSendTo.setOnClickListener(this);

        mEditTextSubject = (EditText) findViewById(R.id.compose_editText_subject);

        mEditTextContent = (EditText) findViewById(R.id.compose_editText_content);
        mEditTextContent.setCursorVisible(false);
        mEditTextContent.setOnClickListener(this);

        mAttachment = (ImageView) findViewById(R.id.imageView_attachment_send_message_area);
        mAttachment.setVisibility(View.GONE);
        mDeleteAttachment = (ImageView) findViewById(R.id.imageView_close_attachment_send_message_area);
        mDeleteAttachment.setOnClickListener(this);
        mDeleteAttachment.setVisibility(View.GONE);


        // init search contacts view
        mSearchContactRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_search_contacts);
        mSearchContactRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSearchContactRecyclerView.addItemDecoration(new GridSpacingItemDecoration(1, dpToPx(1), true));
        mSearchContactRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mSearchContactRecyclerView.setVisibility(View.GONE);
        mSearchContactRecyclerView.setItemViewCacheSize(20);

        imageUtils = new ImagePicker(this);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.compose_menu, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.compose_editText_content:
                mEditTextContent.setCursorVisible(true);
                break;

            case R.id.compose_editText_email:
                mSearchContactArrayList = mDataManager.getInboxDB().getUserList(mDataManager);
                mSearchListAdapter = new SearchListAdapter(mSearchContactArrayList);
                mSearchContactRecyclerView.setAdapter(mSearchListAdapter);

                mSearchContactRecyclerView.setVisibility(View.VISIBLE);
                search();
                break;

            case R.id.imageView_close_attachment_send_message_area:
                mAttachment.setVisibility(View.GONE);
                mDeleteAttachment.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                onBackPressed();
                break;

            case R.id.action_send:
                 sendMessage();
                break;

            case R.id.action_attachment:
                imagepicker();

                break;

            default:
                return false;
        }
        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    loadedUri = result.getUri();
                    loadedBitmap = uriToBitmap(loadedUri);
                    loadedBitmap = ImageConverter.scaleDownSaveRatio(loadedBitmap,(float)0.3,true);
                    setSmallImageInAttachment(loadedBitmap);
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
                break;

            case CAMERA_REQUEST:
                if(resultCode == RESULT_OK) {
                    //to get thumb image
//                    loadedBitmap = (Bitmap) data.getExtras().get("data");
//                    setSmallImageInAttachment(loadedBitmap);

                    //to get full size image
                    Uri uri = Uri.fromFile(new File(imageUtils.getCurrentPhotoPath()));
                    //loadedBitmap = BitmapFactory.decodeFile(imageUtils.getCurrentPhotoPath());
                    cropImage(uri);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        imageUtils.request_permission_result(requestCode, permissions, grantResults);
    }

    private void sendMessage() {

        String subject = mEditTextSubject.getText().toString();
        String content = mEditTextContent.getText().toString();
        String sendTo = mEditTextSendTo.getText().toString();


        int validCode = checkValid(sendTo,subject,content);

        switch (validCode){
            case 1:
                // problem with email
                createAlertDialog("Ooops...","Email is not valid");
                break;
            case 2:
                // subject is empty
                createAlertDialog("Ooops...","Please fill up the subject");
                break;
            case 3:
                // no content and no image
                createAlertDialog("Ooops...","Message is empty");
                break;
            case 0:

                // get uuid
                UuidGenerator uuidGenerator = new UuidGenerator();
                // all good
                String message = subject+"\n "+content;
                UUID uuidDestination = null;
                UUID uuidSender =  mDataManager.getNodesDB().getMyNodeId();

                // todo delete when no needed
                // Admin backdoor to create demo messages
                if(subject.contains("admin")){
                    message = subject.split("iamadmin")[0]+"\n "+content;
                    String sender = subject.split("iamadmin")[1];
                    try {
                        uuidSender =uuidGenerator.GenerateUUIDFromEmail(sender);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ////////////////////////////////////

                try {
                     uuidDestination =uuidGenerator.GenerateUUIDFromEmail(sendTo);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //check if there is an image to upload
                if (mAttachment.getVisibility() == View.VISIBLE){
                    // add relay message with attachment with image
                    if (loadedBitmap != null) {

                        RelayMessage newMessage = new RelayMessage(
                                uuidSender, uuidDestination, RelayMessage.TYPE_MESSAGE_INCLUDE_ATTACHMENT,
                                message, ImageConverter.ConvertBitmapToBytes(loadedBitmap));
                        mDataManager.getMessagesDB().addMessage(newMessage);
                        requestForSearch();
                    }
                }
                else{
                    if (content.trim().length() != 0) {
                        RelayMessage newMessage = new RelayMessage(
                                uuidSender, uuidDestination, RelayMessage.TYPE_MESSAGE_TEXT,
                                message, null);
                        mDataManager.getMessagesDB().addMessage(newMessage);
                        requestForSearch();
                    }
                }
                mEditTextContent.setText("");
                mEditTextSendTo.setText("");
                mEditTextSubject.setText("");
                mAttachment.setVisibility(View.GONE);
                mDeleteAttachment.setVisibility(View.GONE);
                onBackPressed();
                break;
        }
    }

    public void setSmallImageInAttachment(Bitmap image){
        // set image in attachment
        // create small pic with low resolution
        Bitmap smallPic;

        smallPic =ImageConverter.scaleDownToSquare(image,300,true);
        smallPic = ImageConverter.getRoundedCornerBitmap(smallPic,10);

        mAttachment.setImageBitmap(smallPic);
        mAttachment.setVisibility(View.VISIBLE);
        mDeleteAttachment.setVisibility(View.VISIBLE);
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

    /**
     * Converting dp to pixel
     */
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    public int checkValid(String email, String subject, String content){

        UuidGenerator uuidGenerator = new UuidGenerator();

        try {
            uuidGenerator.GenerateUUIDFromEmail(email);
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }

        if (subject.trim().length() == 0)
            return 2;

        if (content.trim().length() == 0 && mAttachment.getVisibility() == View.GONE)
            return 3;

        return 0;
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

    /**
     *  Create alert dialog
     */
    private void createAlertDialog(String title,String msg) {

        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public class SearchListAdapter extends SearchContactAdapter {

        public SearchListAdapter(ArrayList<SearchUser> arrayList) {
            super(arrayList);
        }

        @Override
        public SearchContactAdapter.SearchViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_contact_search_result, viewGroup, false);
            return new SearchViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SearchContactAdapter.SearchViewHolder viewHolder,final int i) {

            viewHolder.contact.setText(mFilteredList.get(i).getEmail());
            viewHolder.arrow.setVisibility(View.GONE);

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mEditTextSendTo.setText(mFilteredList.get(i).getEmail());
                    mSearchContactRecyclerView.setVisibility(View.GONE);
                }
            });
        }

    }

    // search for user  or name or email
    private void search() {

        mEditTextSendTo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(final CharSequence charSequence, int i, int i1, int i2) {
                mSearchListAdapter.getFilter().filter(charSequence);
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mEditTextSendTo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(final TextView textView, int i, KeyEvent keyEvent) {
                mSearchContactRecyclerView.setVisibility(View.GONE);
                return false;
            }
        });

        mEditTextSendTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b)
                    mSearchContactRecyclerView.setVisibility(View.GONE);
            }
        });

    }
    public void cropImage(Uri uri){
        if (uri != null){
            // crop image
            CropImage.activity(uri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);
        }
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


    private void setRatioImageInAttachment() {
        Bitmap bitmap = ImageConverter.scaleImageToImageViewSize(mAttachment,imageUtils.getCurrentPhotoPath());
		/* Associate the Bitmap to the ImageView */
        mAttachment.setImageBitmap(bitmap);
        mAttachment.setVisibility(View.VISIBLE);
        mDeleteAttachment.setVisibility(View.VISIBLE);
    }
}
