package com.relay.relay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.relay.relay.SubSystem.DataManager;
import com.relay.relay.Util.CountryCodeActivityDialog;
import com.relay.relay.Util.ImageConverter;
import com.relay.relay.Util.Imageutils;
import com.relay.relay.Util.ShowActivityFullImage;
import com.relay.relay.system.Node;
import com.theartofdev.edmodo.cropper.CropImageActivity;
import com.theartofdev.edmodo.cropper.CropImageOptions;

import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;
import static com.relay.relay.SignInActivity.CURRENT_UUID_USER;
import static com.relay.relay.MainActivity.SYSTEM_SETTING;
import static com.relay.relay.Util.CountryCodeActivityDialog.ACTION_OPEN;
import static com.relay.relay.Util.Imageutils.CAMERA_REQUEST;
import static com.relay.relay.Util.Imageutils.GALLERY_REQUEST;
import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_EXTRA_OPTIONS;
import static com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_EXTRA_SOURCE;


public class ProfileFragment extends Fragment implements View.OnClickListener {

    private final String TAG = "RELAY_DEBUG: "+ ProfileFragment.class.getSimpleName();

    private final int CODE_FULL_NAME = 11;
    private final int CODE_USER_NAME = 14;
    private final int CODE_USER_PHONE = 16;

    private Menu mMenu;
    private Node userNode;
    private Bitmap mUserImage;
    private String mUserFullname;
    private String mUserEmail;
    private String mUserName;
    private String mUserPhone;
    private String mUserResidence;
    private String mUserProfileShort;
    private int countryCode;
    private String userUUID; // the user profile
    private String myUUID;

    // answer from dialog
    String inputAnswer;

    //For Image Attachment
    private Imageutils imageUtils;
    private Bitmap loadedBitmap;
    private Uri loadedUri;


    // fragment view
    private View view = null;
    private ImageView mEditProfileImage;
    private de.hdodenhof.circleimageview.CircleImageView mProfileImage;
    private TextView mTextViewUserFullname;
    private TextView mTextViewUserEmail;
    private TextView mTextViewUserName;
    private TextView mTextViewUserPhone;
    private TextView mTextViewUserResidence;
    private TextView mTextViewUserProfileName;
    private TextView mTextViewUserProfileShort;
    private Button mSetFullname;
    private Button mSetUserName;
    private Button mSetPhone;
    private Button mSetResidence;

    // database
    DataManager mDataManager;

    private OnFragmentInteractionListener mListener;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String userUUID) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("userUUID", userUUID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG,"On create");
        // To enable editing the tool bar from fragment
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            userUUID = getArguments().getString("userUUID");
        }

        mDataManager = new DataManager(getContext());

        SharedPreferences sharedPreferences =  getActivity().getSharedPreferences(SYSTEM_SETTING,0);
        myUUID = sharedPreferences.getString(CURRENT_UUID_USER,null);

        userNode = mDataManager.getNodesDB().getNode(UUID.fromString(userUUID));
        mUserImage = ImageConverter.convertBytesToBitmap(userNode.getProfilePicture());
        mUserFullname = userNode.getFullName();
        mUserEmail = userNode.getEmail();
        mUserName = userNode.getUserName();
        mUserPhone = userNode.getPhoneNumber();
        mUserResidence = CountryCodeActivityDialog.getCountryFromCode(userNode.getResidenceCode());
        mUserProfileShort = "@"+mUserName.toLowerCase()+",  "+mUserEmail;

        imageUtils = new Imageutils(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.e(TAG,"onCreateView");
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_profile, container, false);

        mEditProfileImage = (ImageView) view.findViewById(R.id.profile_fragment_edit_profile_image);

        mProfileImage =  (de.hdodenhof.circleimageview.CircleImageView) view.findViewById(R.id.profile_fragment_user_profile_photo);
        if (mUserImage != null)
            mProfileImage.setImageBitmap(mUserImage);
        else
            mProfileImage.setImageDrawable(getResources().getDrawable(R.drawable.pic_unknown_user));
        mProfileImage.setOnClickListener(this);

        mTextViewUserFullname = (TextView) view.findViewById(R.id.text_view_profile_fragment_user_full_name);
        mTextViewUserFullname.setText(mUserFullname);
        mSetFullname = (Button)  view.findViewById(R.id.button_profile_fragment_user_full_name);

        mTextViewUserEmail = (TextView) view.findViewById(R.id.text_view_profile_fragment_user_email);
        mTextViewUserEmail.setText(mUserEmail);

        mTextViewUserName = (TextView) view.findViewById(R.id.text_view_profile_fragment_user_name);
        mTextViewUserName.setText(mUserName);
        mSetUserName = (Button)  view.findViewById(R.id.button_profile_fragment_user_name);

        mTextViewUserPhone = (TextView) view.findViewById(R.id.text_view_profile_fragment_user_phone);
        mTextViewUserPhone.setText(mUserPhone);
        mSetPhone = (Button)  view.findViewById(R.id.button_profile_fragment_user_phone);

        mTextViewUserResidence = (TextView) view.findViewById(R.id.text_view_profile_fragment_user_residence);
        mTextViewUserResidence.setText(mUserResidence );
        mSetResidence = (Button)  view.findViewById(R.id.button_profile_fragment_user_residence);

        mTextViewUserProfileName =(TextView) view.findViewById(R.id.profile_fragment_user_profile_name);
        mTextViewUserProfileName.setText(mUserFullname);

        mTextViewUserProfileShort = (TextView) view.findViewById(R.id.profile_fragment_user_profile_short);
        mTextViewUserProfileShort.setText(mUserProfileShort);



        if (myUUID.equals(userUUID)){
            mEditProfileImage.setOnClickListener(this);
            mTextViewUserFullname.setOnClickListener(this);
            mTextViewUserName.setOnClickListener(this);
            mTextViewUserResidence.setOnClickListener(this);
            mTextViewUserPhone.setOnClickListener(this);
            mSetFullname.setOnClickListener(this);
            mSetUserName.setOnClickListener(this);
            mSetPhone.setOnClickListener(this);
            mSetResidence.setOnClickListener(this);

        }
        else{
            mEditProfileImage.setVisibility(View.GONE);
            mTextViewUserFullname.setBackgroundResource(android.R.color.transparent);
            mTextViewUserResidence.setBackgroundResource(android.R.color.transparent);
            mTextViewUserName.setBackgroundResource(android.R.color.transparent);
            mTextViewUserPhone.setBackgroundResource(android.R.color.transparent);
            mSetFullname.setVisibility(View.INVISIBLE);
            mSetUserName.setVisibility(View.INVISIBLE);
            mSetPhone.setVisibility(View.INVISIBLE);
            mSetResidence.setVisibility(View.INVISIBLE);
        }


        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {

        switch(view.getId()){

            case R.id.profile_fragment_edit_profile_image:
                    imagepicker();
                break;

            case R.id.profile_fragment_user_profile_photo:
                new ShowActivityFullImage(mUserImage,getActivity());
                break;


            case R.id.button_profile_fragment_user_full_name:
                openDialogWithInputText("Edit full name",mUserFullname,CODE_FULL_NAME);
                break;

            case R.id.text_view_profile_fragment_user_full_name:
                openDialogWithInputText("Edit full name",mUserFullname,CODE_FULL_NAME);
                break;

            case R.id.button_profile_fragment_user_residence:
                openListDialog();
                break;
            case R.id.text_view_profile_fragment_user_residence:
                openListDialog();
                break;

            case R.id.button_profile_fragment_user_name:
                openDialogWithInputText("Edit user name",mUserName,CODE_USER_NAME);
                break;
            case R.id.text_view_profile_fragment_user_name:
                openDialogWithInputText("Edit user name",mUserName,CODE_USER_NAME);
                break;

            case R.id.button_profile_fragment_user_phone:
                openDialogWithInputText("Edit phone number",mUserPhone,CODE_USER_PHONE);
                break;
            case R.id.text_view_profile_fragment_user_phone:
                openDialogWithInputText("Edit phone number",mUserPhone,CODE_USER_PHONE);
                break;

        }
    }

    private void openListDialog() {
        final Intent intent = new Intent(getContext(), CountryCodeActivityDialog.class);
        startActivityForResult(intent, ACTION_OPEN);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_manual_handshake).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_approve).setVisible(myUUID.equals(userUUID));

        // save current menu;
        mMenu = menu;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_approve) {
            Snackbar.make(view, "Changes were saved" , Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            //update nodeDB
            userNode.setTimeStampNodeDetails(Calendar.getInstance());
            mDataManager.getNodesDB().addNode(userNode);
            mDataManager.closeAllDataBase();
            getActivity().onBackPressed();
            return true;
        }

        if (id == android.R.id.home) {
            getActivity().onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void cropImage(Uri uri){
        if (uri != null){
            // crop image
            Intent intent = new Intent();
            intent.setClass(getContext(), CropImageActivity.class);
            intent.putExtra(CROP_IMAGE_EXTRA_SOURCE, uri);
            intent.putExtra(CROP_IMAGE_EXTRA_OPTIONS, new CropImageOptions());
            startActivity(intent);
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

        android.app.AlertDialog.Builder alertdialog = new android.app.AlertDialog.Builder(getContext());
        alertdialog.setTitle("Add Image");
        alertdialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Camera")) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null)
                        startActivityForResult(takePictureIntent, CAMERA_REQUEST);

                }
                else if (items[item].equals("Gallery")) {

                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(intent, GALLERY_REQUEST);
                }
            }
        });
        alertdialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        imageUtils.request_permission_result(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode,data);

        switch (requestCode) {

            case CAMERA_REQUEST:
                if(resultCode == RESULT_OK) {
                    loadedBitmap = (Bitmap) data.getExtras().get("data");
                    mUserImage = loadedBitmap;
                    loadedBitmap = ImageConverter.scaleDown(loadedBitmap,100,true);
                    mProfileImage.setImageBitmap(loadedBitmap);
                    updateProfileImage(loadedBitmap);

                }
                break;

            case GALLERY_REQUEST:
                if(resultCode== RESULT_OK && data != null) {
                    Log.i("Gallery","Photo");

                    loadedUri = data.getData();
                    loadedBitmap = uriToBitmap(loadedUri);
                    mUserImage = loadedBitmap;
                    loadedBitmap = ImageConverter.scaleDown(loadedBitmap,100,true);
                    mProfileImage.setImageBitmap(loadedBitmap);
                    mUserImage = loadedBitmap;
                    updateProfileImage(loadedBitmap);

                }
                break;
            case ACTION_OPEN:
                if(resultCode == RESULT_OK) {
                    countryCode = data.getIntExtra(CountryCodeActivityDialog.RESULT_CONTRYCODE, 1);
                    mUserResidence = CountryCodeActivityDialog.getCountryFromCode(countryCode);
                    mTextViewUserResidence.setText(mUserResidence);
                    userNode.setResidenceCode(countryCode);
                }
                break;
        }
    }

    public void updateProfileImage(Bitmap image) {
        userNode.setProfilePicture(ImageConverter.ConvertBitmapToBytes(image),true);
    }

    public Bitmap uriToBitmap(Uri uri){

        Bitmap bitmap = null;
        try {
            bitmap =  MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void openDialogWithInputText(String title,String oldInput,final int code){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title);

        // Set up the input
        final EditText editTextInput = new EditText(getContext());
        editTextInput.setText(oldInput);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        editTextInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        builder.setView(editTextInput);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = editTextInput.getText().toString();
                if (!checkValidInput(input,code))
                    dialog.dismiss();
                else{
                    //inputAnswer = input;

                    switch(code){
                        case CODE_FULL_NAME:
                            userNode.setFullName(mUserFullname);
                            break;
                        case CODE_USER_NAME:
                            userNode.setUserName(mUserName);
                            break;
                        case CODE_USER_PHONE:
                            userNode.setPhoneNumber(mUserPhone);
                            break;
                    }
                    //update nodeDB
                    mTextViewUserFullname.setText(mUserFullname);
                    mTextViewUserProfileName.setText(mUserFullname);
                    mTextViewUserResidence.setText(mUserResidence );
                    mTextViewUserName.setText(mUserName);
                    mUserProfileShort = "@"+mUserName.toLowerCase()+",  "+mUserEmail;
                    mTextViewUserProfileShort.setText(mUserProfileShort);
                    mTextViewUserPhone.setText(mUserPhone);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    public boolean checkValidInput(String input,int inputCode){

        String newInput = "";
        String[] inptArray;
        switch(inputCode){
            case CODE_FULL_NAME:
                if (input.trim().length() == 0){
                    createAlertDialog("Input error","please Enter first and last name");
                    return false;
                }


                inptArray = input.split(" ");
                if (inptArray.length < 2){
                    createAlertDialog("Input error","please Enter first and last name");
                    return false;
                }
                for(int i =0 ;i< inptArray.length; i++){
                    char ch = inptArray[i].charAt(0);
                    newInput = newInput + String.valueOf(ch).toUpperCase() +inptArray[i].substring(1) +" ";
                }
                // cutoff the last space
                newInput = newInput.substring(0,newInput.length()-1);
                mUserFullname = newInput;
                return true;

            case CODE_USER_NAME:
                if (input.trim().length() == 0){
                    createAlertDialog("Input error","please enter user name");
                    return false;
                }
                newInput = "";
                inptArray = input.split(" ");
                if (inptArray.length > 1){
                    createAlertDialog("Input error","please Enter valid username \nwithout spaces");
                    return false;
                }
                mUserName = input;
                return true;

            case CODE_USER_PHONE:
                // todo fix the check
                if (input.trim().length() == 0){
                    createAlertDialog("Input error","please phone numver");
                    return false;
                }
                newInput = "";
                mUserPhone = input;
                return true;
        }

        return false;
    }

    /**
     *  Create alert dialog
     */
    private void createAlertDialog(String title,String msg) {

        android.support.v7.app.AlertDialog alertDialog = new android.support.v7.app.AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();
    }
}
